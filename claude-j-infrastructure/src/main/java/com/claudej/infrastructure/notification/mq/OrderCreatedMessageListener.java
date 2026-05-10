package com.claudej.infrastructure.notification.mq;

import com.claudej.application.notification.service.NotificationApplicationService;
import com.claudej.application.order.dto.OrderCreatedMessageDTO;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedMessageListener {

    private final NotificationApplicationService notificationApplicationService;

    public OrderCreatedMessageListener(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    public void onOrderCreated(OrderCreatedMessageDTO message) {
        notificationApplicationService.handleOrderCreated(message);
    }
}
