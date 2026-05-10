package com.claudej.infrastructure.notification.sender;

import com.claudej.application.notification.port.NotificationSender;
import com.claudej.domain.notification.model.aggregate.Notification;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {
        // D1 only persists processing result; external delivery stays out of scope.
    }
}
