package com.sandbox.infrastructure.notifications;

import com.sandbox.notifications.domain.model.Notification;
import com.sandbox.notifications.domain.port.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(Notification notification) {
        log.info("[{}] To: {} | Subject: {} | Body: {}",
                notification.channel(), notification.recipient(), notification.subject(), notification.body());
    }
}
