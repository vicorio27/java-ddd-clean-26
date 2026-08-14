package com.sandbox.infrastructure.persistence.orders;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    @Version
    private long version;

    // updatable = false: la fecha de creacion se fija una vez y ningun save posterior la pisa.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    protected OrderJpaEntity() {
    }

    public static OrderJpaEntity newOrder(UUID id, UUID customerId) {
        var entity = new OrderJpaEntity();
        entity.id = id;
        entity.customerId = customerId;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderLineJpaEntity> getLines() {
        return List.copyOf(lines);
    }

    /** Muta la coleccion existente para no romper orphanRemoval de Hibernate. */
    public void replaceLines(Collection<OrderLineJpaEntity> newLines) {
        this.lines.clear();
        this.lines.addAll(newLines);
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
