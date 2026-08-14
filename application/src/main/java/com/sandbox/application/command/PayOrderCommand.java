package com.sandbox.application.command;

import java.util.Objects;

public record PayOrderCommand(String orderId, String idempotencyKey) {

    public PayOrderCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    }
}
