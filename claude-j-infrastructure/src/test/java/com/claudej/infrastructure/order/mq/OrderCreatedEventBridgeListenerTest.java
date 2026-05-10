package com.claudej.infrastructure.order.mq;

import com.claudej.application.order.dto.OrderCreatedMessage;
import com.claudej.application.order.port.OrderMessagePublisher;
import com.claudej.domain.order.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventBridgeListenerTest {

    @Mock
    private OrderCreatedMessageAssembler orderCreatedMessageAssembler;

    @Mock
    private OrderMessagePublisher orderMessagePublisher;

    @InjectMocks
    private OrderCreatedEventBridgeListener orderCreatedEventBridgeListener;

    @Test
    void should_publishOrderCreatedMessage_when_orderCreatedEventHandled() {
        OrderCreatedEvent event = OrderCreatedEvent.create("ORD-401", "CUST-401", Collections.singletonList(
                new com.claudej.domain.order.event.OrderItemInfo("PROD-401", "Phone", 1)
        ));
        org.mockito.Mockito.when(orderCreatedMessageAssembler.toMessage(any(OrderCreatedEvent.class), any(java.math.BigDecimal.class), any(String.class)))
                .thenReturn(new OrderCreatedMessage());

        orderCreatedEventBridgeListener.onOrderCreated(event);

        verify(orderCreatedMessageAssembler).toMessage(any(OrderCreatedEvent.class), any(java.math.BigDecimal.class), any(String.class));
        verify(orderMessagePublisher).publishOrderCreated(any(OrderCreatedMessage.class));
    }
}
