package com.claudej.application.notification.service;

import com.claudej.application.notification.port.NotificationSender;
import com.claudej.application.order.dto.OrderCreatedMessageDTO;
import com.claudej.domain.notification.model.aggregate.Notification;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.notification.model.valueobject.NotificationStatus;
import com.claudej.domain.notification.repository.NotificationRepository;
import com.claudej.domain.order.model.valobj.OrderId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private NotificationApplicationService notificationApplicationService;

    @Test
    void should_saveSentNotification_when_messageConsumedSuccessfully() {
        OrderCreatedMessageDTO message = createMessage();
        when(notificationRepository.findByOrderIdAndChannel(new OrderId("ORD-001"), NotificationChannel.INTERNAL))
                .thenReturn(Optional.empty());

        notificationApplicationService.handleOrderCreated(message);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(any(Notification.class));
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void should_skipWhen_notificationAlreadyExistsForOrder() {
        OrderCreatedMessageDTO message = createMessage();
        Notification existing = Notification.createPending(
                new OrderId("ORD-001"),
                NotificationChannel.INTERNAL,
                com.claudej.domain.notification.model.valueobject.NotificationPayload.of(
                        new OrderId("ORD-001"), "CUST-001", new BigDecimal("88.00"), "existing")
        );
        when(notificationRepository.findByOrderIdAndChannel(new OrderId("ORD-001"), NotificationChannel.INTERNAL))
                .thenReturn(Optional.of(existing));

        notificationApplicationService.handleOrderCreated(message);

        verify(notificationSender, never()).send(any(Notification.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void should_saveFailedNotification_when_senderThrowsException() {
        OrderCreatedMessageDTO message = createMessage();
        when(notificationRepository.findByOrderIdAndChannel(new OrderId("ORD-001"), NotificationChannel.INTERNAL))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new IllegalStateException("send failed")).when(notificationSender).send(any(Notification.class));

        assertThatThrownBy(() -> notificationApplicationService.handleOrderCreated(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("send failed");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    private OrderCreatedMessageDTO createMessage() {
        OrderCreatedMessageDTO message = new OrderCreatedMessageDTO();
        message.setEventId("evt-001");
        message.setOrderId("ORD-001");
        message.setCustomerId("CUST-001");
        message.setTotalAmount(new BigDecimal("88.00"));
        message.setCurrency("CNY");
        message.setOccurredOn(java.time.LocalDateTime.of(2026, 4, 30, 12, 0, 0));
        return message;
    }
}
