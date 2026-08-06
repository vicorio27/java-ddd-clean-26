package com.sandbox.payments.domain.event;

import com.sandbox.payments.domain.model.PaymentId;
import com.sandbox.shared.kernel.event.DomainEvent;

import java.math.BigDecimal;

public record PaymentCompletedEvent(PaymentId paymentId, String orderReference, BigDecimal amount)
        implements DomainEvent {
}
