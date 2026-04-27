package com.claudej.domain.payment.event;

import com.claudej.domain.order.event.OrderItemInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PaymentSuccessEvent tests
 */
class PaymentSuccessEventTest {

    private List<OrderItemInfo> createTestItems() {
        return Arrays.asList(
                new OrderItemInfo("PROD001", "Product 1", 2),
                new OrderItemInfo("PROD002", "Product 2", 1)
        );
    }

    @Test
    void should_create_event_with_valid_params() {
        LocalDateTime now = LocalDateTime.now();
        List<OrderItemInfo> items = createTestItems();
        PaymentSuccessEvent event = new PaymentSuccessEvent(
                "EVT001", now, "PAY001", "ORD001", "CUS001", "TXN001", items
        );

        assertThat(event.getEventId()).isEqualTo("EVT001");
        assertThat(event.getOccurredOn()).isEqualTo(now);
        assertThat(event.getAggregateType()).isEqualTo("Payment");
        assertThat(event.getAggregateId()).isEqualTo("PAY001");
        assertThat(event.getPaymentId()).isEqualTo("PAY001");
        assertThat(event.getOrderId()).isEqualTo("ORD001");
        assertThat(event.getCustomerId()).isEqualTo("CUS001");
        assertThat(event.getTransactionNo()).isEqualTo("TXN001");
        assertThat(event.getItems()).hasSize(2);
        assertThat(event.getItems().get(0).getProductId()).isEqualTo("PROD001");
        assertThat(event.getItems().get(1).getProductId()).isEqualTo("PROD002");
    }

    @Test
    void should_create_event_via_factory_method() {
        List<OrderItemInfo> items = createTestItems();
        PaymentSuccessEvent event = PaymentSuccessEvent.create("PAY001", "ORD001", "CUS001", "TXN001", items);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getAggregateType()).isEqualTo("Payment");
        assertThat(event.getAggregateId()).isEqualTo("PAY001");
        assertThat(event.getPaymentId()).isEqualTo("PAY001");
        assertThat(event.getOrderId()).isEqualTo("ORD001");
        assertThat(event.getCustomerId()).isEqualTo("CUS001");
        assertThat(event.getTransactionNo()).isEqualTo("TXN001");
        assertThat(event.getItems()).hasSize(2);
    }

    @Test
    void should_generate_unique_event_id_via_factory() {
        List<OrderItemInfo> items = createTestItems();
        PaymentSuccessEvent event1 = PaymentSuccessEvent.create("PAY001", "ORD001", "CUS001", "TXN001", items);
        PaymentSuccessEvent event2 = PaymentSuccessEvent.create("PAY001", "ORD001", "CUS001", "TXN001", items);

        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    void should_throw_when_payment_id_is_null() {
        List<OrderItemInfo> items = createTestItems();
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                null, "ORD001", "CUS001", "TXN001", items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("paymentId must not be null or empty");
    }

    @Test
    void should_throw_when_payment_id_is_empty() {
        List<OrderItemInfo> items = createTestItems();
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "", "ORD001", "CUS001", "TXN001", items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("paymentId must not be null or empty");
    }

    @Test
    void should_throw_when_order_id_is_null() {
        List<OrderItemInfo> items = createTestItems();
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "PAY001", null, "CUS001", "TXN001", items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderId must not be null or empty");
    }

    @Test
    void should_throw_when_order_id_is_empty() {
        List<OrderItemInfo> items = createTestItems();
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "PAY001", "", "CUS001", "TXN001", items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderId must not be null or empty");
    }

    @Test
    void should_throw_when_customer_id_is_null() {
        List<OrderItemInfo> items = createTestItems();
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "PAY001", "ORD001", null, "TXN001", items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerId must not be null or empty");
    }

    @Test
    void should_throw_when_customer_id_is_empty() {
        List<OrderItemInfo> items = createTestItems();
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "PAY001", "ORD001", "", "TXN001", items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerId must not be null or empty");
    }

    @Test
    void should_throw_when_transaction_no_is_null() {
        List<OrderItemInfo> items = createTestItems();
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "PAY001", "ORD001", "CUS001", null, items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("transactionNo must not be null or empty");
    }

    @Test
    void should_throw_when_transaction_no_is_empty() {
        List<OrderItemInfo> items = createTestItems();
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "PAY001", "ORD001", "CUS001", "", items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("transactionNo must not be null or empty");
    }

    @Test
    void should_throw_when_items_is_null() {
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "PAY001", "ORD001", "CUS001", "TXN001", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must not be null or empty");
    }

    @Test
    void should_throw_when_items_is_empty() {
        assertThatThrownBy(() -> new PaymentSuccessEvent("EVT001", LocalDateTime.now(),
                "PAY001", "ORD001", "CUS001", "TXN001", Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must not be null or empty");
    }

    @Test
    void should_defensively_copy_items_list() {
        List<OrderItemInfo> items = new java.util.ArrayList<>(createTestItems());
        PaymentSuccessEvent event = PaymentSuccessEvent.create("PAY001", "ORD001", "CUS001", "TXN001", items);

        // Original list modification should not affect event
        items.add(new OrderItemInfo("PROD003", "Product 3", 1));

        assertThat(event.getItems()).hasSize(2);
    }

    @Test
    void should_return_unmodifiable_items_list() {
        List<OrderItemInfo> items = createTestItems();
        PaymentSuccessEvent event = PaymentSuccessEvent.create("PAY001", "ORD001", "CUS001", "TXN001", items);

        assertThatThrownBy(() -> event.getItems().add(
                new OrderItemInfo("PROD003", "Product 3", 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
