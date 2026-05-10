package com.claudej.infrastructure.order.mq;

import com.claudej.application.order.dto.OrderCreatedMessage;
import com.claudej.domain.order.event.OrderCreatedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderCreatedMessageAssembler {

    public OrderCreatedMessage toMessage(OrderCreatedEvent event, BigDecimal totalAmount, String currency) {
        OrderCreatedMessage message = new OrderCreatedMessage();
        message.setEventId(event.getEventId());
        message.setOrderId(event.getOrderId());
        message.setCustomerId(event.getCustomerId());
        message.setTotalAmount(totalAmount);
        message.setCurrency(currency);
        message.setOccurredOn(event.getOccurredOn());
        return message;
    }
}
