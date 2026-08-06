package com.sandbox.notifications.domain.model;

import java.util.Objects;

public record Notification(String recipient, String subject, String body, Channel channel) {

    public Notification {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
    }

    public enum Channel {
        EMAIL, SMS, PUSH
    }
}
