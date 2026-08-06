package com.sandbox.inventory.domain.port;

import com.sandbox.inventory.domain.model.StockItem;

import java.util.Optional;

public interface InventoryRepository {

    Optional<StockItem> findByProductId(String productId);

    StockItem save(StockItem item);
}
