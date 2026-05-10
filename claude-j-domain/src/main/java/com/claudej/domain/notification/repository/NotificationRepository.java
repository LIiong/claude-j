package com.claudej.domain.notification.repository;

import com.claudej.domain.notification.model.aggregate.Notification;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.order.model.valobj.OrderId;

import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findByOrderIdAndChannel(OrderId orderId, NotificationChannel channel);
}
