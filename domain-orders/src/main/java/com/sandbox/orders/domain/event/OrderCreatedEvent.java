package com.sandbox.orders.domain.event;

import com.sandbox.orders.domain.model.OrderId;
import com.sandbox.shared.kernel.event.DomainEvent;
import com.sandbox.shared.kernel.id.CustomerId;

import java.math.BigDecimal;

public record OrderCreatedEvent(OrderId orderId, CustomerId customerId, BigDecimal totalAmount, String currency)
        implements DomainEvent {
}
