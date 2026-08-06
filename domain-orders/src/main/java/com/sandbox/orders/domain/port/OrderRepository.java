package com.sandbox.orders.domain.port;

import com.sandbox.orders.domain.model.Order;
import com.sandbox.orders.domain.model.OrderId;

import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);
}
