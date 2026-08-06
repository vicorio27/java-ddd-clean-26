package com.sandbox.payments.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PaymentId(UUID value) {

    public PaymentId {
        Objects.requireNonNull(value, "PaymentId value must not be null");
    }

    public static PaymentId newId() {
        return new PaymentId(UUID.randomUUID());
    }
}
