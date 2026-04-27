package com.claudej.domain.order.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OrderCancelledEvent tests
 */
class OrderCancelledEventTest {

    @Test
    void should_create_event_with_valid_params() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        LocalDateTime occurredOn = LocalDateTime.now();
        String orderId = "ORDER001";
        String customerId = "CUST001";
        List<OrderItemInfo> items = Arrays.asList(
                new OrderItemInfo("PROD001", "iPhone", 1)
        );

        // Act
        OrderCancelledEvent event = new OrderCancelledEvent(eventId, occurredOn, orderId, customerId, items);

        // Assert
        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getOccurredOn()).isEqualTo(occurredOn);
        assertThat(event.getAggregateType()).isEqualTo("Order");
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void should_create_event_using_factory_method() {
        // Arrange
        String orderId = "ORDER001";
        String customerId = "CUST001";
        List<OrderItemInfo> items = Collections.singletonList(
                new OrderItemInfo("PROD001", "iPhone", 1)
        );

        // Act
        OrderCancelledEvent event = OrderCancelledEvent.create(orderId, customerId, items);

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void should_throw_when_order_id_is_null() {
        assertThatThrownBy(() -> OrderCancelledEvent.create(null, "CUST001",
                Collections.singletonList(new OrderItemInfo("PROD001", "iPhone", 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId");
    }

    @Test
    void should_throw_when_items_is_empty() {
        assertThatThrownBy(() -> OrderCancelledEvent.create("ORDER001", "CUST001", Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("items");
    }
}
