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
import java.util.concurrent.StructuredTaskScope;

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

    public OrderDetailsView execute(String orderId) {
        var id = OrderId.of(orderId);
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.allSuccessfulOrThrow())) {
            var orderTask = scope.fork(() -> orderRepository.findById(id)
                    .orElseThrow(() -> new DomainException("Order not found: " + orderId)));
            scope.join();

            var order = orderTask.get();
            var customerTask = scope.fork(() -> customerRepository.findById(order.customerId()));
            var paymentTask = scope.fork(() -> paymentRepository.findByOrderReference(order.id().value().toString()));
            scope.join();

            return new OrderDetailsView(order, customerTask.get(), paymentTask.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException("Interrupted while loading order details");
        }
    }

    public record OrderDetailsView(Order order, Optional<Customer> customer, Optional<Payment> payment) {
    }
}
