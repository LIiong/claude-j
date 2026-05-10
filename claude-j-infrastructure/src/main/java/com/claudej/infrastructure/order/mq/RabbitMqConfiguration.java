package com.claudej.infrastructure.order.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfiguration {

    @Bean
    public DirectExchange orderCreatedExchange(RabbitMqProperties rabbitMqProperties) {
        return new DirectExchange(rabbitMqProperties.getOrderCreatedExchange());
    }

    @Bean
    public Queue orderCreatedQueue(RabbitMqProperties rabbitMqProperties) {
        return new Queue(rabbitMqProperties.getOrderCreatedQueue(), true);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue,
                                       DirectExchange orderCreatedExchange,
                                       RabbitMqProperties rabbitMqProperties) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(orderCreatedExchange)
                .with(rabbitMqProperties.getOrderCreatedRoutingKey());
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
