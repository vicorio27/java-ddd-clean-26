package com.sandbox.shared.kernel.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    default UUID eventId() {
        return UUID.randomUUID();
    }

    default Instant occurredOn() {
        return Instant.now();
    }
}
