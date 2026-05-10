package com.claudej.application.notification.port;

import com.claudej.domain.notification.model.aggregate.Notification;

public interface NotificationSender {

    void send(Notification notification);
}
