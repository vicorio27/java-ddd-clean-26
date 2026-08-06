package com.sandbox.orders.domain.port;

import com.sandbox.shared.kernel.event.DomainEvent;

public interface OrderEventPublisher {

    void publish(DomainEvent event);
}
