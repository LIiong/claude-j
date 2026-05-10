package com.claudej.infrastructure.order.mq;

import com.claudej.application.order.dto.OrderCreatedMessage;
import com.claudej.application.order.port.OrderMessagePublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqOrderMessagePublisher implements OrderMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;

    public RabbitMqOrderMessagePublisher(RabbitTemplate rabbitTemplate,
                                         RabbitMqProperties rabbitMqProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqProperties = rabbitMqProperties;
    }

    @Override
    public void publishOrderCreated(OrderCreatedMessage message) {
        rabbitTemplate.convertAndSend(
                rabbitMqProperties.getOrderCreatedExchange(),
                rabbitMqProperties.getOrderCreatedRoutingKey(),
                message
        );
    }
}
