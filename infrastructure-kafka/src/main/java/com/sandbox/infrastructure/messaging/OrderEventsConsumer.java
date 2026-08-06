package com.sandbox.infrastructure.messaging;

import com.sandbox.notifications.domain.model.Notification;
import com.sandbox.notifications.domain.port.NotificationSender;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);

    private final NotificationSender notificationSender;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public OrderEventsConsumer(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    @KafkaListener(topics = "${sandbox.kafka.topics.orders:orders.events}", groupId = "notifications")
    public void onOrderEvent(ConsumerRecord<String, Object> record) {
        if (!processedEventIds.add(record.key())) {
            log.info("Duplicate event {} ignored (idempotent consumer)", record.key());
            return;
        }
        log.info("Consumed order event {}: {}", record.key(), record.value());
        notificationSender.send(new Notification(
                "orders-team@sandbox.local",
                "Order event received",
                String.valueOf(record.value()),
                Notification.Channel.EMAIL));
    }
}
