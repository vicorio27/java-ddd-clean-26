package com.sandbox.infrastructure.messaging;

import com.sandbox.application.port.OutboxStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay del patron outbox: la unica ruta por la que un evento de dominio llega a Kafka.
 *
 * <p>El caso de uso escribe el evento en la misma transaccion que el agregado; este
 * componente lo publica despues. Si Kafka esta caido, la fila sigue sin marcar y se
 * reintenta en el siguiente ciclo: se garantiza entrega at-least-once, y por eso el
 * consumidor deduplica por eventId.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore outboxStore;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String ordersTopic;
    private final int batchSize;

    public OutboxRelay(OutboxStore outboxStore,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @Value("${sandbox.kafka.topics.orders:orders.events}") String ordersTopic,
                       @Value("${sandbox.outbox.batch-size:100}") int batchSize) {
        this.outboxStore = outboxStore;
        this.kafkaTemplate = kafkaTemplate;
        this.ordersTopic = ordersTopic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${sandbox.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        for (var record : outboxStore.fetchUnpublished(batchSize)) {
            try {
                // Envio sincrono: solo se marca como publicado lo que el broker confirmo.
                kafkaTemplate.send(ordersTopic, record.eventId().toString(), record.payload()).get();
                outboxStore.markPublished(record.eventId());
                log.debug("Relayed outbox event {} ({})", record.eventId(), record.eventType());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Se deja sin marcar a proposito: el siguiente ciclo lo reintenta.
                log.warn("Could not relay outbox event {}, will retry", record.eventId(), e);
                return;
            }
        }
    }
}
