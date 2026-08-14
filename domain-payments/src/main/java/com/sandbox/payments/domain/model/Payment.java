package com.sandbox.payments.domain.model;

import com.sandbox.payments.domain.event.PaymentCompletedEvent;
import com.sandbox.shared.kernel.exception.DomainException;
import com.sandbox.shared.kernel.model.AggregateRoot;
import com.sandbox.shared.kernel.money.Money;

public final class Payment extends AggregateRoot {

    private final PaymentId id;
    private final String orderReference;
    private final Money amount;
    private final String idempotencyKey;
    private PaymentStatus status;

    private Payment(PaymentId id, String orderReference, Money amount, String idempotencyKey, PaymentStatus status) {
        this.id = id;
        this.orderReference = orderReference;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
    }

    public static Payment initiate(String orderReference, Money amount, String idempotencyKey) {
        if (orderReference == null || orderReference.isBlank()) {
            throw new DomainException("A payment requires an order reference");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new DomainException("A payment requires an idempotency key");
        }
        return new Payment(PaymentId.newId(), orderReference, amount, idempotencyKey, PaymentStatus.PENDING);
    }

    public static Payment reconstitute(PaymentId id, String orderReference, Money amount,
                                       String idempotencyKey, PaymentStatus status) {
        return new Payment(id, orderReference, amount, idempotencyKey, status);
    }

    public void complete() {
        if (status != PaymentStatus.PENDING) {
            throw new DomainException("Only pending payments can be completed");
        }
        this.status = PaymentStatus.COMPLETED;
        registerEvent(PaymentCompletedEvent.of(id, orderReference, amount.amount()));
    }

    public void fail() {
        if (status != PaymentStatus.PENDING) {
            throw new DomainException("Only pending payments can fail");
        }
        this.status = PaymentStatus.FAILED;
    }

    public PaymentId id() {
        return id;
    }

    public String orderReference() {
        return orderReference;
    }

    public Money amount() {
        return amount;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public PaymentStatus status() {
        return status;
    }
}
