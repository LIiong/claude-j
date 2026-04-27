package com.claudej.infrastructure.inventory.event;

import com.claudej.application.inventory.service.InventoryApplicationService;
import com.claudej.domain.order.event.OrderCancelledEvent;
import com.claudej.domain.order.event.OrderCreatedEvent;
import com.claudej.domain.order.event.OrderItemInfo;
import com.claudej.domain.payment.event.PaymentRefundedEvent;
import com.claudej.domain.payment.event.PaymentSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Inventory event listener - listens to domain events and manages inventory
 */
@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryApplicationService inventoryApplicationService;

    public InventoryEventListener(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    /**
     * Handle order created event - reserve stock
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Handling OrderCreatedEvent for order: {}", event.getOrderId());
        try {
            List<OrderItemInfo> items = event.getItems();
            for (OrderItemInfo item : items) {
                inventoryApplicationService.reserveStock(item.getProductId(), item.getQuantity());
                log.info("Reserved stock for product: {}, quantity: {}",
                        item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            log.error("Failed to reserve stock for order: {}", event.getOrderId(), e);
            // Don't rethrow - event listener failure should not affect main transaction
        }
    }

    /**
     * Handle payment success event - deduct stock (reserved -> deducted)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("Handling PaymentSuccessEvent for order: {}", event.getOrderId());
        try {
            List<OrderItemInfo> items = event.getItems();
            for (OrderItemInfo item : items) {
                inventoryApplicationService.deductStock(item.getProductId(), item.getQuantity());
                log.info("Deducted stock for product: {}, quantity: {}",
                        item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            log.error("Failed to deduct stock for order: {}", event.getOrderId(), e);
            // Don't rethrow - event listener failure should not affect main transaction
        }
    }

    /**
     * Handle order cancelled event - release reserved stock
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Handling OrderCancelledEvent for order: {}", event.getOrderId());
        try {
            List<OrderItemInfo> items = event.getItems();
            for (OrderItemInfo item : items) {
                inventoryApplicationService.releaseStock(item.getProductId(), item.getQuantity());
                log.info("Released stock for product: {}, quantity: {}",
                        item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            log.error("Failed to release stock for order: {}", event.getOrderId(), e);
        }
    }

    /**
     * Handle payment refunded event - restore stock
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        log.info("Handling PaymentRefundedEvent for order: {}", event.getOrderId());
        try {
            List<OrderItemInfo> items = event.getItems();
            for (OrderItemInfo item : items) {
                // Use adjustStock to restore available stock (for refunded orders)
                inventoryApplicationService.adjustStock(item.getProductId(), item.getQuantity());
                log.info("Restored stock for product: {}, quantity: {}",
                        item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            log.error("Failed to restore stock for order: {}", event.getOrderId(), e);
            // Don't rethrow - event listener failure should not affect main transaction
        }
    }
}
