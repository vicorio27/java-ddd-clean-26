package com.sandbox.application.usecase;

import com.sandbox.application.command.PayOrderCommand;
import com.sandbox.application.port.UnitOfWork;
import com.sandbox.orders.domain.model.OrderId;
import com.sandbox.orders.domain.port.OrderRepository;
import com.sandbox.payments.domain.model.Payment;
import com.sandbox.payments.domain.model.PaymentStatus;
import com.sandbox.payments.domain.port.PaymentGateway;
import com.sandbox.payments.domain.port.PaymentRepository;
import com.sandbox.shared.kernel.exception.DomainException;

public class PayOrderUseCase {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final UnitOfWork unitOfWork;

    public PayOrderUseCase(OrderRepository orderRepository,
                           PaymentRepository paymentRepository,
                           PaymentGateway paymentGateway,
                           UnitOfWork unitOfWork) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.unitOfWork = unitOfWork;
    }

    /**
     * Cobrar es la operacion menos reintentable del sistema, asi que se parte en tres pasos:
     *
     * <ol>
     *   <li>Transaccion 1: reservar la clave de idempotencia creando el pago en PENDING.
     *       La unicidad la garantiza el indice de {@code payments.idempotency_key}, no el codigo.</li>
     *   <li>Llamada al gateway <em>fuera</em> de transaccion: una llamada de red no debe
     *       mantener abierta una transaccion de base de datos.</li>
     *   <li>Transaccion 2: registrar el resultado y el nuevo estado de la orden en un solo commit.</li>
     * </ol>
     *
     * <p>Repetir la peticion con la misma clave devuelve el pago existente sin volver a cobrar.
     */
    public Payment execute(PayOrderCommand command) {
        var alreadyProcessed = unitOfWork.execute(
                () -> paymentRepository.findByIdempotencyKey(command.idempotencyKey()));
        if (alreadyProcessed.isPresent()) {
            return alreadyProcessed.get();
        }

        var pending = unitOfWork.execute(() -> paymentRepository
                .findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> {
                    var order = orderRepository.findById(OrderId.of(command.orderId()))
                            .orElseThrow(() -> new DomainException("Order not found: " + command.orderId()));
                    return paymentRepository.save(Payment.initiate(
                            order.id().value().toString(), order.total(), command.idempotencyKey()));
                }));

        if (pending.status() != PaymentStatus.PENDING) {
            return pending;
        }

        var result = paymentGateway.charge(pending);

        return unitOfWork.execute(() -> {
            var order = orderRepository.findById(OrderId.of(command.orderId()))
                    .orElseThrow(() -> new DomainException("Order not found: " + command.orderId()));

            if (result.approved()) {
                pending.complete();
                order.confirm();
                order.markAsPaid();
            } else {
                pending.fail();
            }

            orderRepository.save(order);
            return paymentRepository.save(pending);
        });
    }
}
