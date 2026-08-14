package com.sandbox.infrastructure.persistence.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sandbox.orders.domain.port.OrderEventPublisher;
import com.sandbox.shared.kernel.event.DomainEvent;
import com.sandbox.shared.kernel.exception.DomainException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventPublisher implements OrderEventPublisher {

    private final OutboxJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * MANDATORY es deliberado: publicar un evento fuera de la transaccion del caso de uso
     * reintroduciria el dual-write que el outbox existe para eliminar. Si alguien llama a
     * este metodo sin transaccion activa, el arranque del flujo falla en vez de perder eventos.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        try {
            repository.save(new OutboxEventJpaEntity(
                    event.eventId(),
                    aggregateTypeOf(event),
                    aggregateIdOf(event),
                    event.eventType(),
                    objectMapper.writeValueAsString(event),
                    event.occurredOn()));
        } catch (JsonProcessingException e) {
            throw new DomainException("Cannot serialize domain event " + event.eventType() + ": " + e.getMessage());
        }
    }

    private String aggregateTypeOf(DomainEvent event) {
        return event.eventType().replaceAll("(Created|Completed|Cancelled|Failed)Event$", "");
    }

    private String aggregateIdOf(DomainEvent event) {
        return event.eventId().toString();
    }
}
