package com.sandbox.notifications.domain.port;

import com.sandbox.notifications.domain.model.Notification;

public interface NotificationSender {

    void send(Notification notification);
}
