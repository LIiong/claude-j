package com.claudej.infrastructure.order.mq;

import com.claudej.application.order.dto.OrderCreatedMessageDTO;
import com.claudej.domain.order.event.OrderCreatedEvent;
import com.claudej.domain.order.event.OrderItemInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCreatedMessageAssemblerTest {

    private final OrderCreatedMessageAssembler assembler = new OrderCreatedMessageAssembler();

    @Test
    void should_mapOrderCreatedEventToMessage_when_eventProvided() {
        OrderCreatedEvent event = OrderCreatedEvent.create(
                "ORD-201",
                "CUST-201",
                Collections.singletonList(new OrderItemInfo("PROD-1", "Phone", 2))
        );

        OrderCreatedMessageDTO message = assembler.toMessage(event, new java.math.BigDecimal("123.00"), "CNY");

        assertThat(message.getEventId()).isEqualTo(event.getEventId());
        assertThat(message.getOrderId()).isEqualTo("ORD-201");
        assertThat(message.getCustomerId()).isEqualTo("CUST-201");
        assertThat(message.getTotalAmount()).isEqualByComparingTo("123.00");
        assertThat(message.getCurrency()).isEqualTo("CNY");
    }
}
