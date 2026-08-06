package com.sandbox.orders.domain.model;

import com.sandbox.orders.domain.event.OrderCreatedEvent;
import com.sandbox.shared.kernel.exception.DomainException;
import com.sandbox.shared.kernel.id.CustomerId;
import com.sandbox.shared.kernel.model.AggregateRoot;
import com.sandbox.shared.kernel.money.Money;

import java.util.List;

public final class Order extends AggregateRoot {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private OrderStatus status;
    private long version;

    private Order(OrderId id, CustomerId customerId, List<OrderLine> lines, OrderStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.lines = List.copyOf(lines);
        this.status = status;
    }

    public static Order create(CustomerId customerId, List<OrderLine> lines) {
        if (customerId == null) {
            throw new DomainException("An order requires a customer");
        }
        if (lines == null || lines.isEmpty()) {
            throw new DomainException("An order requires at least one line");
        }
        var order = new Order(OrderId.newId(), customerId, lines, OrderStatus.CREATED);
        var total = order.total();
        order.registerEvent(new OrderCreatedEvent(
                order.id, customerId, total.amount(), total.currency().getCurrencyCode()));
        return order;
    }

    public static Order reconstitute(OrderId id, CustomerId customerId, List<OrderLine> lines,
                                     OrderStatus status, long version) {
        var order = new Order(id, customerId, lines, status);
        order.version = version;
        return order;
    }

    public Money total() {
        return lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money::add)
                .orElseThrow(() -> new DomainException("Cannot compute total of an empty order"));
    }

    public void confirm() {
        transitionTo(OrderStatus.CONFIRMED);
    }

    public void markAsPaid() {
        transitionTo(OrderStatus.PAID);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new DomainException("Illegal order state transition: " + status + " -> " + target);
        }
        this.status = target;
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public OrderStatus status() {
        return status;
    }

    public long version() {
        return version;
    }
}
