package com.claudej.infrastructure.order.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "claudej.rabbitmq")
public class RabbitMqProperties {

    @NotBlank
    private String orderCreatedExchange;

    @NotBlank
    private String orderCreatedQueue;

    @NotBlank
    private String orderCreatedRoutingKey;

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
