package com.sandbox.orders.domain.model;

import com.sandbox.shared.kernel.exception.DomainException;
import com.sandbox.shared.kernel.money.Money;

import java.util.Objects;

public record OrderLine(String productId, int quantity, Money unitPrice) {

    public OrderLine {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new DomainException("Order line quantity must be positive");
        }
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
