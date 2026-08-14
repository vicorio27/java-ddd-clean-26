package com.sandbox.infrastructure.persistence.outbox;

import com.sandbox.application.port.OutboxStore;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class JpaOutboxStore implements OutboxStore {

    private final OutboxJpaRepository repository;

    public JpaOutboxStore(OutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxRecord> fetchUnpublished(int limit) {
        return repository.findByPublishedFalseOrderByOccurredOnAsc(Limit.of(limit)).stream()
                .map(entity -> new OutboxRecord(
                        entity.getId(),
                        entity.getAggregateType(),
                        entity.getAggregateId(),
                        entity.getEventType(),
                        entity.getPayload(),
                        entity.getOccurredOn()))
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(UUID eventId) {
        repository.findById(eventId).ifPresent(OutboxEventJpaEntity::markPublished);
    }
}
