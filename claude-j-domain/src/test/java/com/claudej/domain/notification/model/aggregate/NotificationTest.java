package com.claudej.domain.notification.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.notification.model.valueobject.NotificationPayload;
import com.claudej.domain.notification.model.valueobject.NotificationStatus;
import com.claudej.domain.order.model.valobj.OrderId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void should_createPendingNotification_when_payloadIsValid() {
        Notification notification = Notification.createPending(
                new OrderId("ORD-001"),
                NotificationChannel.INTERNAL,
                NotificationPayload.of(new OrderId("ORD-001"), "CUST-001", new BigDecimal("99.00"), "created")
        );

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    void should_markSent_when_notificationIsPending() {
        Notification notification = Notification.createPending(
                new OrderId("ORD-001"),
                NotificationChannel.INTERNAL,
                NotificationPayload.of(new OrderId("ORD-001"), "CUST-001", new BigDecimal("99.00"), "created")
        );
        LocalDateTime sentAt = LocalDateTime.of(2026, 4, 30, 10, 0, 0);

        notification.markSent(sentAt);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isEqualTo(sentAt);
    }

    @Test
    void should_markFailed_when_notificationIsPending() {
        Notification notification = Notification.createPending(
                new OrderId("ORD-001"),
                NotificationChannel.INTERNAL,
                NotificationPayload.of(new OrderId("ORD-001"), "CUST-001", new BigDecimal("99.00"), "created")
        );

        notification.markFailed();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void should_throwBusinessException_when_markFailedAfterSent() {
        Notification notification = Notification.createPending(
                new OrderId("ORD-001"),
                NotificationChannel.INTERNAL,
                NotificationPayload.of(new OrderId("ORD-001"), "CUST-001", new BigDecimal("99.00"), "created")
        );
        notification.markSent(LocalDateTime.now());

        assertThatThrownBy(notification::markFailed)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_CANNOT_MARK_FAILED);
    }
}
