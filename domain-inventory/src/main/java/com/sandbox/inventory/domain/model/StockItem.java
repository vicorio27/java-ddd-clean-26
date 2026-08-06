package com.sandbox.inventory.domain.model;

import com.sandbox.inventory.domain.exception.InsufficientStockException;

public final class StockItem {

    private final String productId;
    private int availableQuantity;

    private StockItem(String productId, int availableQuantity) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
    }

    public static StockItem of(String productId, int availableQuantity) {
        return new StockItem(productId, availableQuantity);
    }

    public void reserve(int quantity) {
        if (quantity > availableQuantity) {
            throw new InsufficientStockException(productId, quantity, availableQuantity);
        }
        this.availableQuantity -= quantity;
    }

    public void release(int quantity) {
        this.availableQuantity += quantity;
    }

    public String productId() {
        return productId;
    }

    public int availableQuantity() {
        return availableQuantity;
    }
}
