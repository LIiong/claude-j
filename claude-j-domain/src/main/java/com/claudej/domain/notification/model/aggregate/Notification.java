package com.claudej.domain.notification.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.notification.model.valueobject.NotificationId;
import com.claudej.domain.notification.model.valueobject.NotificationPayload;
import com.claudej.domain.notification.model.valueobject.NotificationStatus;
import com.claudej.domain.order.model.valobj.OrderId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 通知聚合根
 */
@Getter
public class Notification {

    private Long id;
    private final NotificationId notificationId;
    private final OrderId orderId;
    private final NotificationChannel channel;
    private NotificationStatus status;
    private final NotificationPayload payload;
    private LocalDateTime sentAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Notification(NotificationId notificationId, OrderId orderId, NotificationChannel channel,
                         NotificationStatus status, NotificationPayload payload,
                         LocalDateTime sentAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.notificationId = notificationId;
        this.orderId = orderId;
        this.channel = channel;
        this.status = status;
        this.payload = payload;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Notification createPending(OrderId orderId, NotificationChannel channel, NotificationPayload payload) {
        validate(orderId, channel, payload);
        LocalDateTime now = LocalDateTime.now();
        return new Notification(
                new NotificationId(UUID.randomUUID().toString().replace("-", "")),
                orderId,
                channel,
                NotificationStatus.PENDING,
                payload,
                null,
                now,
                now
        );
    }

    public static Notification reconstruct(Long id, NotificationId notificationId, OrderId orderId,
                                           NotificationChannel channel, NotificationStatus status,
                                           NotificationPayload payload, LocalDateTime sentAt,
                                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        validate(orderId, channel, payload);
        if (notificationId == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ID_EMPTY);
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_STATUS_INVALID);
        }
        Notification notification = new Notification(notificationId, orderId, channel, status, payload,
                sentAt, createdAt, updatedAt);
        notification.id = id;
        return notification;
    }

    public void markSent(LocalDateTime sentAt) {
        if (status != NotificationStatus.PENDING && status != NotificationStatus.FAILED) {
            throw new BusinessException(ErrorCode.NOTIFICATION_CANNOT_MARK_SENT);
        }
        this.status = NotificationStatus.SENT;
        this.sentAt = sentAt == null ? LocalDateTime.now() : sentAt;
        this.updatedAt = this.sentAt;
    }

    public void markFailed() {
        if (status == NotificationStatus.SENT) {
            throw new BusinessException(ErrorCode.NOTIFICATION_CANNOT_MARK_FAILED);
        }
        this.status = NotificationStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void setId(Long id) {
        this.id = id;
    }

    private static void validate(OrderId orderId, NotificationChannel channel, NotificationPayload payload) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ORDER_ID_EMPTY);
        }
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_CHANNEL_INVALID);
        }
        if (payload == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_PAYLOAD_EMPTY);
        }
    }
}
