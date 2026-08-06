package com.sandbox.infrastructure.messaging;

import com.sandbox.orders.domain.port.OrderEventPublisher;
import com.sandbox.shared.kernel.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ordersTopic;

    public KafkaOrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                    @Value("${sandbox.kafka.topics.orders:orders.events}") String ordersTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordersTopic = ordersTopic;
    }

    @Override
    public void publish(DomainEvent event) {
        kafkaTemplate.send(ordersTopic, event.eventId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to {}", event.eventId(), ordersTopic, ex);
                    } else {
                        log.info("Published event {} to {}", event.eventId(), ordersTopic);
                    }
                });
    }
}
