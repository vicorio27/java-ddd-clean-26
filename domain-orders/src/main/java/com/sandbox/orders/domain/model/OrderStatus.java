package com.sandbox.orders.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {

    CREATED,
    CONFIRMED,
    PAID,
    SHIPPED,
    CANCELLED;

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(CREATED, CONFIRMED);

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED || target == CANCELLED;
            case SHIPPED, CANCELLED -> false;
        };
    }

    public boolean isCancellable() {
        return CANCELLABLE.contains(this);
    }
}
