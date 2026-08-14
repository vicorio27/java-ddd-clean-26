package com.sandbox.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lectura del outbox para el relay que publica a Kafka.
 * La escritura ocurre dentro de la transaccion del caso de uso (ver OutboxEventPublisher).
 */
public interface OutboxStore {

    List<OutboxRecord> fetchUnpublished(int limit);

    void markPublished(UUID eventId);

    record OutboxRecord(UUID eventId,
                        String aggregateType,
                        String aggregateId,
                        String eventType,
                        String payload,
                        Instant occurredOn) {
    }
}
