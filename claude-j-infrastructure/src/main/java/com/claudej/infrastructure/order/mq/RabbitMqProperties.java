package com.claudej.infrastructure.order.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "claudej.rabbitmq")
public class RabbitMqProperties {

    @NotBlank
    private String orderCreatedExchange = "order.created.exchange";

    @NotBlank
    private String orderCreatedQueue = "order.created.queue";

    @NotBlank
    private String orderCreatedRoutingKey = "order.created";

    public String getOrderCreatedExchange() {
        return orderCreatedExchange;
    }

    public void setOrderCreatedExchange(String orderCreatedExchange) {
        this.orderCreatedExchange = orderCreatedExchange;
    }

    public String getOrderCreatedQueue() {
        return orderCreatedQueue;
    }

    public void setOrderCreatedQueue(String orderCreatedQueue) {
        this.orderCreatedQueue = orderCreatedQueue;
    }

    public String getOrderCreatedRoutingKey() {
        return orderCreatedRoutingKey;
    }

    public void setOrderCreatedRoutingKey(String orderCreatedRoutingKey) {
        this.orderCreatedRoutingKey = orderCreatedRoutingKey;
    }
}
