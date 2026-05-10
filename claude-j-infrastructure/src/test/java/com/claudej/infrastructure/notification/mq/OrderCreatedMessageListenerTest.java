package com.claudej.infrastructure.notification.mq;

import com.claudej.application.notification.service.NotificationApplicationService;
import com.claudej.application.order.dto.OrderCreatedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCreatedMessageListenerTest {

    @Mock
    private NotificationApplicationService notificationApplicationService;

    @InjectMocks
    private OrderCreatedMessageListener orderCreatedMessageListener;

    @Test
    void should_delegateToNotificationApplicationService_when_messageReceived() {
        OrderCreatedMessage message = new OrderCreatedMessage();
        message.setEventId("evt-1");
        message.setOrderId("ORD-301");
        message.setCustomerId("CUST-301");
        message.setTotalAmount(new BigDecimal("56.00"));
        message.setCurrency("CNY");
        message.setOccurredOn(LocalDateTime.of(2026, 4, 30, 12, 0, 0));

        orderCreatedMessageListener.onOrderCreated(message);

        verify(notificationApplicationService).handleOrderCreated(message);
    }
}
