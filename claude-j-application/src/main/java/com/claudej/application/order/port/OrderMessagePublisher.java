package com.claudej.application.order.port;

import com.claudej.application.order.dto.OrderCreatedMessage;

public interface OrderMessagePublisher {

    void publishOrderCreated(OrderCreatedMessage message);
}
