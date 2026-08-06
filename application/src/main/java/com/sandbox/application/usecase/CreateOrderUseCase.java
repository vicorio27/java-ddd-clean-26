package com.sandbox.application.usecase;

import com.sandbox.application.command.CreateOrderCommand;
import com.sandbox.customers.domain.port.CustomerRepository;
import com.sandbox.orders.domain.model.Order;
import com.sandbox.orders.domain.model.OrderLine;
import com.sandbox.orders.domain.port.OrderEventPublisher;
import com.sandbox.orders.domain.port.OrderRepository;
import com.sandbox.orders.domain.service.OrderDomainService;
import com.sandbox.shared.kernel.exception.DomainException;
import com.sandbox.shared.kernel.id.CustomerId;
import com.sandbox.shared.kernel.money.Money;

public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderEventPublisher eventPublisher;
    private final OrderDomainService orderDomainService;

    public CreateOrderUseCase(OrderRepository orderRepository,
                              CustomerRepository customerRepository,
                              OrderEventPublisher eventPublisher,
                              OrderDomainService orderDomainService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
        this.orderDomainService = orderDomainService;
    }

    public Order execute(CreateOrderCommand command) {
        var customerId = CustomerId.of(command.customerId());
        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new DomainException("Customer not found: " + command.customerId()));
        if (!customer.canPlaceOrders()) {
            throw new DomainException("Customer is not allowed to place orders");
        }

        var lines = command.lines().stream()
                .map(line -> new OrderLine(line.productId(), line.quantity(),
                        Money.of(line.unitPrice(), line.currency())))
                .toList();

        var order = Order.create(customerId, lines);
        orderDomainService.assertCanBeCreated(order);

        var saved = orderRepository.save(order);
        saved.pullDomainEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
