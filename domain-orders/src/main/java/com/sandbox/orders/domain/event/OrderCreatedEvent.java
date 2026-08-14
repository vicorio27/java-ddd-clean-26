package com.sandbox.orders.domain.event;

import com.sandbox.orders.domain.model.OrderId;
import com.sandbox.shared.kernel.event.DomainEvent;
import com.sandbox.shared.kernel.id.CustomerId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(UUID eventId,
                                Instant occurredOn,
                                OrderId orderId,
                                CustomerId customerId,
                                BigDecimal totalAmount,
                                String currency) implements DomainEvent {

    public static OrderCreatedEvent of(OrderId orderId, CustomerId customerId, BigDecimal totalAmount, String currency) {
        return new OrderCreatedEvent(UUID.randomUUID(), Instant.now(), orderId, customerId, totalAmount, currency);
    }
}
