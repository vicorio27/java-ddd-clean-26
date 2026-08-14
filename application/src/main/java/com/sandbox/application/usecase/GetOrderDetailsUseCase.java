package com.sandbox.application.usecase;

import com.sandbox.customers.domain.model.Customer;
import com.sandbox.customers.domain.port.CustomerRepository;
import com.sandbox.orders.domain.model.Order;
import com.sandbox.orders.domain.model.OrderId;
import com.sandbox.orders.domain.port.OrderRepository;
import com.sandbox.payments.domain.model.Payment;
import com.sandbox.payments.domain.port.PaymentRepository;
import com.sandbox.shared.kernel.exception.DomainException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class GetOrderDetailsUseCase {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;

    public GetOrderDetailsUseCase(OrderRepository orderRepository,
                                  CustomerRepository customerRepository,
                                  PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * La version anterior usaba StructuredTaskScope y estaba rota por partida doble:
     * hacia fork() despues de join() (IllegalStateException garantizada en ejecucion) y,
     * al ser API preview, obligaba a --enable-preview, lo que emite bytecode que ArchUnit
     * no sabe leer. Un executor de hilos virtuales da el mismo paralelismo sin API preview.
     */
    public OrderDetailsView execute(String orderId) {
        var order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new DomainException("Order not found: " + orderId));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var customer = executor.submit(() -> customerRepository.findById(order.customerId()));
            var payment = executor.submit(() -> paymentRepository.findByOrderReference(
                    order.id().value().toString()));

            return new OrderDetailsView(order, customer.get(), payment.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException("Interrupted while loading order details");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof DomainException domainException) {
                throw domainException;
            }
            throw new DomainException("Could not load order details: " + e.getCause().getMessage());
        }
    }

    public record OrderDetailsView(Order order, Optional<Customer> customer, Optional<Payment> payment) {
    }
}
