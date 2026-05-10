package com.claudej.infrastructure.order.mq;

import com.claudej.application.order.dto.OrderCreatedMessageDTO;
import com.claudej.application.order.port.OrderMessagePublisher;
import com.claudej.domain.order.event.OrderCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Component
public class OrderCreatedEventBridgeListener {

    private final OrderCreatedMessageAssembler orderCreatedMessageAssembler;
    private final OrderMessagePublisher orderMessagePublisher;

    public OrderCreatedEventBridgeListener(OrderCreatedMessageAssembler orderCreatedMessageAssembler,
                                           OrderMessagePublisher orderMessagePublisher) {
        this.orderCreatedMessageAssembler = orderCreatedMessageAssembler;
        this.orderMessagePublisher = orderMessagePublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        OrderCreatedMessageDTO message = orderCreatedMessageAssembler.toMessage(event, BigDecimal.ZERO, "CNY");
        orderMessagePublisher.publishOrderCreated(message);
    }
}
