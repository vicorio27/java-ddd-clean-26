package com.sandbox.shared.kernel.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Todo evento de dominio lleva identidad estable.
 *
 * <p>La version anterior definia {@code default UUID eventId() { return UUID.randomUUID(); }},
 * de modo que cada invocacion devolvia un id distinto: la clave de Kafka, la fila del outbox
 * y la deduplicacion del consumidor nunca podian coincidir. La identidad ahora es un dato del
 * evento, no un efecto secundario de leerlo.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredOn();

    default String eventType() {
        return getClass().getSimpleName();
    }
}
