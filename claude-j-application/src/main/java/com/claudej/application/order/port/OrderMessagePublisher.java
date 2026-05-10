package com.claudej.application.order.port;

import com.claudej.application.order.dto.OrderCreatedMessageDTO;

public interface OrderMessagePublisher {

    void publishOrderCreated(OrderCreatedMessageDTO message);
}
