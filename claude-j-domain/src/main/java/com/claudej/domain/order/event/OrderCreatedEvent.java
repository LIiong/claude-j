package com.claudej.domain.order.event;

import com.claudej.domain.common.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Order created domain event - triggered when order is successfully created
 */
@Getter
public class OrderCreatedEvent extends DomainEvent {

    private final String orderId;
    private final String customerId;
    private final List<OrderItemInfo> items;

    public OrderCreatedEvent(String eventId, LocalDateTime occurredOn, String orderId, String customerId, List<OrderItemInfo> items) {
        super(eventId, occurredOn, "Order", validateOrderId(orderId));
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("customerId must not be null or empty");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be null or empty");
        }
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    private static String validateOrderId(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("orderId must not be null or empty");
        }
        return orderId;
    }

    /**
     * Factory method to create event with auto-generated eventId and occurredOn
     */
    public static OrderCreatedEvent create(String orderId, String customerId, List<OrderItemInfo> items) {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                orderId,
                customerId,
                items
        );
    }
}
