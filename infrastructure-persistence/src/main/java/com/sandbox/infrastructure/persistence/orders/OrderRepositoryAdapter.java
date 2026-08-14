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

    /**
     * La version anterior construia siempre una entidad nueva, de modo que:
     * la version de @Version volvia a 0 (el optimistic locking anunciado nunca
     * llegaba a la base de datos) y created_at se reescribia con Instant.now()
     * en cada actualizacion. Ahora se carga la fila existente y se muta.
     */
    @Override
    @Transactional
    public Order save(Order order) {
        var entity = jpaRepository.findById(order.id().value())
                .orElseGet(() -> OrderJpaEntity.newOrder(order.id().value(), order.customerId().value()));

        entity.setStatus(order.status().name());
        entity.replaceLines(order.lines().stream()
                .map(line -> new OrderLineJpaEntity(
                        line.productId(),
                        line.quantity(),
                        line.unitPrice().amount(),
                        line.unitPrice().currency().getCurrencyCode()))
                .toList());

        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    private Order toDomain(OrderJpaEntity entity) {
        var lines = entity.getLines().stream()
                .map(line -> new OrderLine(line.getProductId(), line.getQuantity(),
                        Money.of(line.getUnitPrice(), line.getCurrency())))
                .toList();
        return Order.reconstitute(new OrderId(entity.getId()), new CustomerId(entity.getCustomerId()),
                lines, OrderStatus.valueOf(entity.getStatus()), entity.getVersion());
    }
}
