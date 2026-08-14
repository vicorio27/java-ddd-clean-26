package com.sandbox.payments.domain.event;

import com.sandbox.payments.domain.model.PaymentId;
import com.sandbox.shared.kernel.event.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(UUID eventId,
                                    Instant occurredOn,
                                    PaymentId paymentId,
                                    String orderReference,
                                    BigDecimal amount) implements DomainEvent {

    public static PaymentCompletedEvent of(PaymentId paymentId, String orderReference, BigDecimal amount) {
        return new PaymentCompletedEvent(UUID.randomUUID(), Instant.now(), paymentId, orderReference, amount);
    }
}
