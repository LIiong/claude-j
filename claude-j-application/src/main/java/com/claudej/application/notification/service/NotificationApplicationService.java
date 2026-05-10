package com.claudej.application.notification.service;

import com.claudej.application.notification.port.NotificationSender;
import com.claudej.application.order.dto.OrderCreatedMessageDTO;
import com.claudej.domain.notification.model.aggregate.Notification;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.notification.model.valueobject.NotificationPayload;
import com.claudej.domain.notification.repository.NotificationRepository;
import com.claudej.domain.order.model.valobj.OrderId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    public NotificationApplicationService(NotificationRepository notificationRepository,
                                          NotificationSender notificationSender) {
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedMessageDTO message) {
        OrderId orderId = new OrderId(message.getOrderId());
        NotificationChannel channel = NotificationChannel.INTERNAL;
        Optional<Notification> existing = notificationRepository.findByOrderIdAndChannel(orderId, channel);
        if (existing.isPresent()) {
            return;
        }

        Notification notification = Notification.createPending(
                orderId,
                channel,
                NotificationPayload.of(orderId, message.getCustomerId(), message.getTotalAmount(),
                        buildMessage(message.getOrderId(), message.getCustomerId()))
        );

        try {
            notificationSender.send(notification);
            notification.markSent(LocalDateTime.now());
        } catch (RuntimeException ex) {
            notification.markFailed();
            notificationRepository.save(notification);
            throw ex;
        }

        notificationRepository.save(notification);
    }

    private String buildMessage(String orderId, String customerId) {
        return "Order " + orderId + " created for customer " + customerId;
    }
}
