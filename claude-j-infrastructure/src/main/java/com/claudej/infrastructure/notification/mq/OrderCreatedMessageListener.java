package com.claudej.infrastructure.notification.mq;

import com.claudej.application.notification.service.NotificationApplicationService;
import com.claudej.application.order.dto.OrderCreatedMessage;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedMessageListener {

    private final NotificationApplicationService notificationApplicationService;

    public OrderCreatedMessageListener(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    public void onOrderCreated(OrderCreatedMessage message) {
        notificationApplicationService.handleOrderCreated(message);
    }
}
