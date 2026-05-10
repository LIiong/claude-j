package com.claudej.infrastructure.order.mq;

import com.claudej.application.order.dto.OrderCreatedMessageDTO;
import com.claudej.domain.order.event.OrderCreatedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderCreatedMessageAssembler {

    public OrderCreatedMessageDTO toMessage(OrderCreatedEvent event, BigDecimal totalAmount, String currency) {
        OrderCreatedMessageDTO message = new OrderCreatedMessageDTO();
        message.setEventId(event.getEventId());
        message.setOrderId(event.getOrderId());
        message.setCustomerId(event.getCustomerId());
        message.setTotalAmount(totalAmount);
        message.setCurrency(currency);
        message.setOccurredOn(event.getOccurredOn());
        return message;
    }
}
