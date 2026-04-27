package com.claudej.domain.payment.event;

import com.claudej.domain.common.event.DomainEvent;
import com.claudej.domain.order.event.OrderItemInfo;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Payment success domain event - triggered when payment callback is successfully processed
 */
@Getter
public class PaymentSuccessEvent extends DomainEvent {

    private final String paymentId;
    private final String orderId;
    private final String customerId;
    private final String transactionNo;
    private final List<OrderItemInfo> items;

    public PaymentSuccessEvent(String eventId, LocalDateTime occurredOn, String paymentId,
                               String orderId, String customerId, String transactionNo,
                               List<OrderItemInfo> items) {
        super(eventId, occurredOn, "Payment", validateParam(paymentId, "paymentId"));
        this.paymentId = paymentId;
        this.orderId = validateParam(orderId, "orderId");
        this.customerId = validateParam(customerId, "customerId");
        this.transactionNo = validateParam(transactionNo, "transactionNo");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be null or empty");
        }
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    private static String validateParam(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be null or empty");
        }
        return value;
    }

    /**
     * Factory method to create event with auto-generated eventId and occurredOn
     */
    public static PaymentSuccessEvent create(String paymentId, String orderId, String customerId,
                                              String transactionNo, List<OrderItemInfo> items) {
        return new PaymentSuccessEvent(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                paymentId,
                orderId,
                customerId,
                transactionNo,
                items
        );
    }
}
