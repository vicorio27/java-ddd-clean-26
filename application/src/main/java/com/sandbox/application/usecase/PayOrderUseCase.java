package com.sandbox.application.usecase;

import com.sandbox.orders.domain.model.OrderId;
import com.sandbox.orders.domain.port.OrderRepository;
import com.sandbox.payments.domain.model.Payment;
import com.sandbox.payments.domain.port.PaymentGateway;
import com.sandbox.payments.domain.port.PaymentRepository;
import com.sandbox.shared.kernel.exception.DomainException;

public class PayOrderUseCase {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PayOrderUseCase(OrderRepository orderRepository,
                           PaymentRepository paymentRepository,
                           PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    public Payment execute(String orderId) {
        var order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new DomainException("Order not found: " + orderId));

        var payment = Payment.initiate(order.id().value().toString(), order.total());
        var result = paymentGateway.charge(payment);

        if (result.approved()) {
            payment.complete();
            order.markAsPaid();
        } else {
            payment.fail();
        }

        orderRepository.save(order);
        return paymentRepository.save(payment);
    }
}
