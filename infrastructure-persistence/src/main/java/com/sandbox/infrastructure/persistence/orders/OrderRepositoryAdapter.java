package com.sandbox.infrastructure.persistence.orders;

import com.sandbox.orders.domain.model.Order;
import com.sandbox.orders.domain.model.OrderId;
import com.sandbox.orders.domain.model.OrderLine;
import com.sandbox.orders.domain.model.OrderStatus;
import com.sandbox.orders.domain.port.OrderRepository;
import com.sandbox.shared.kernel.id.CustomerId;
import com.sandbox.shared.kernel.money.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        return toDomain(jpaRepository.save(toEntity(order)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    private OrderJpaEntity toEntity(Order order) {
        var entity = new OrderJpaEntity();
        entity.setId(order.id().value());
        entity.setCustomerId(order.customerId().value());
        entity.setStatus(order.status().name());
        entity.setLines(order.lines().stream().map(line -> {
            var lineEntity = new OrderLineJpaEntity();
            lineEntity.setProductId(line.productId());
            lineEntity.setQuantity(line.quantity());
            lineEntity.setUnitPrice(line.unitPrice().amount());
            lineEntity.setCurrency(line.unitPrice().currency().getCurrencyCode());
            return lineEntity;
        }).toList());
        return entity;
    }

    private Order toDomain(OrderJpaEntity entity) {
        var lines = entity.getLines().stream()
                .map(line -> new OrderLine(line.getProductId(), line.getQuantity(),
                        Money.of(line.getUnitPrice(), line.getCurrency())))
                .toList();
        return Order.reconstitute(new OrderId(entity.getId()), new CustomerId(entity.getCustomerId()),
                lines, OrderStatus.valueOf(entity.getStatus()), 0L);
    }
}
