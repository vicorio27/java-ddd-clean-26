package com.sandbox.inventory.domain.exception;

import com.sandbox.shared.kernel.exception.DomainException;

public class InsufficientStockException extends DomainException {

    public InsufficientStockException(String productId, int requested, int available) {
        super("Insufficient stock for product %s: requested %d, available %d"
                .formatted(productId, requested, available));
    }
}
