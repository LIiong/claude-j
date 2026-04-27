package com.claudej.infrastructure.inventory.event;

import com.claudej.application.inventory.service.InventoryApplicationService;
import com.claudej.domain.order.event.OrderCancelledEvent;
import com.claudej.domain.order.event.OrderCreatedEvent;
import com.claudej.domain.order.event.OrderItemInfo;
import com.claudej.domain.payment.event.PaymentRefundedEvent;
import com.claudej.domain.payment.event.PaymentSuccessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * InventoryEventListener tests
 */
@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock
    private InventoryApplicationService inventoryApplicationService;

    private InventoryEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new InventoryEventListener(inventoryApplicationService);
    }

    @Test
    void should_reserve_stock_when_order_created() {
        // Given
        List<OrderItemInfo> items = Arrays.asList(
                new OrderItemInfo("PROD001", "Product 1", 2),
                new OrderItemInfo("PROD002", "Product 2", 1)
        );
        OrderCreatedEvent event = OrderCreatedEvent.create("ORD001", "CUS001", items);

        // When
        listener.onOrderCreated(event);

        // Then
        ArgumentCaptor<String> productIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(inventoryApplicationService, times(2)).reserveStock(productIdCaptor.capture(), quantityCaptor.capture());

        List<String> capturedProductIds = productIdCaptor.getAllValues();
        List<Integer> capturedQuantities = quantityCaptor.getAllValues();

        assertThat(capturedProductIds).containsExactly("PROD001", "PROD002");
        assertThat(capturedQuantities).containsExactly(2, 1);
    }

    @Test
    void should_deduct_stock_when_payment_success() {
        // Given
        List<OrderItemInfo> items = Arrays.asList(
                new OrderItemInfo("PROD001", "Product 1", 2),
                new OrderItemInfo("PROD002", "Product 2", 1)
        );
        PaymentSuccessEvent event = PaymentSuccessEvent.create("PAY001", "ORD001", "CUS001", "TXN001", items);

        // When
        listener.onPaymentSuccess(event);

        // Then
        ArgumentCaptor<String> productIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(inventoryApplicationService, times(2)).deductStock(productIdCaptor.capture(), quantityCaptor.capture());

        List<String> capturedProductIds = productIdCaptor.getAllValues();
        List<Integer> capturedQuantities = quantityCaptor.getAllValues();

        assertThat(capturedProductIds).containsExactly("PROD001", "PROD002");
        assertThat(capturedQuantities).containsExactly(2, 1);
    }

    @Test
    void should_release_stock_when_order_cancelled() {
        // Given
        List<OrderItemInfo> items = Arrays.asList(
                new OrderItemInfo("PROD001", "Product 1", 2),
                new OrderItemInfo("PROD002", "Product 2", 1)
        );
        OrderCancelledEvent event = OrderCancelledEvent.create("ORD001", "CUS001", items);

        // When
        listener.onOrderCancelled(event);

        // Then
        ArgumentCaptor<String> productIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(inventoryApplicationService, times(2)).releaseStock(productIdCaptor.capture(), quantityCaptor.capture());

        List<String> capturedProductIds = productIdCaptor.getAllValues();
        List<Integer> capturedQuantities = quantityCaptor.getAllValues();

        assertThat(capturedProductIds).containsExactly("PROD001", "PROD002");
        assertThat(capturedQuantities).containsExactly(2, 1);
    }

    @Test
    void should_adjust_stock_when_payment_refunded() {
        // Given
        List<OrderItemInfo> items = Arrays.asList(
                new OrderItemInfo("PROD001", "Product 1", 2),
                new OrderItemInfo("PROD002", "Product 2", 1)
        );
        PaymentRefundedEvent event = PaymentRefundedEvent.create("PAY001", "ORD001", "CUS001", "TXN001", items);

        // When
        listener.onPaymentRefunded(event);

        // Then
        ArgumentCaptor<String> productIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(inventoryApplicationService, times(2)).adjustStock(productIdCaptor.capture(), quantityCaptor.capture());

        List<String> capturedProductIds = productIdCaptor.getAllValues();
        List<Integer> capturedQuantities = quantityCaptor.getAllValues();

        assertThat(capturedProductIds).containsExactly("PROD001", "PROD002");
        assertThat(capturedQuantities).containsExactly(2, 1);
    }

    @Test
    void should_not_throw_when_reserve_stock_fails() {
        // Given
        List<OrderItemInfo> items = Arrays.asList(
                new OrderItemInfo("PROD001", "Product 1", 2)
        );
        OrderCreatedEvent event = OrderCreatedEvent.create("ORD001", "CUS001", items);

        doThrow(new RuntimeException("Stock not available"))
                .when(inventoryApplicationService).reserveStock(anyString(), anyInt());

        // When / Then - should not throw
        listener.onOrderCreated(event);

        verify(inventoryApplicationService).reserveStock("PROD001", 2);
    }

    @Test
    void should_not_throw_when_deduct_stock_fails() {
        // Given
        List<OrderItemInfo> items = Arrays.asList(
                new OrderItemInfo("PROD001", "Product 1", 2)
        );
        PaymentSuccessEvent event = PaymentSuccessEvent.create("PAY001", "ORD001", "CUS001", "TXN001", items);

        doThrow(new RuntimeException("Stock not available"))
                .when(inventoryApplicationService).deductStock(anyString(), anyInt());

        // When / Then - should not throw
        listener.onPaymentSuccess(event);

        verify(inventoryApplicationService).deductStock("PROD001", 2);
    }
}
