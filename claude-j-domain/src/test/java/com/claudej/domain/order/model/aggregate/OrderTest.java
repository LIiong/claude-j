package com.claudej.domain.order.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.model.valobj.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void should_createOrder_when_customerIdProvided() {
        // Given
        CustomerId customerId = new CustomerId("CUST001");

        // When
        Order order = Order.create(customerId);

        // Then
        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getOrderIdValue()).isNotEmpty();
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getItems()).isEmpty();
        assertThat(order.getTotalAmount().isZero()).isTrue();
        assertThat(order.getCreateTime()).isNotNull();
        assertThat(order.getUpdateTime()).isNotNull();
    }

    @Test
    void should_throwException_when_customerIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> Order.create(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户ID不能为空");
    }

    @Test
    void should_addItemAndRecalculateTotal() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        OrderItem item = OrderItem.create("PROD001", "iPhone", 2, Money.cny(5999));

        // When
        order.addItem(item);

        // Then
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount().getAmount().intValue()).isEqualTo(11998);
    }

    @Test
    void should_addMultipleItemsAndRecalculateTotal() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        OrderItem item1 = OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999));
        OrderItem item2 = OrderItem.create("PROD002", "MacBook", 1, Money.cny(12999));

        // When
        order.addItem(item1);
        order.addItem(item2);

        // Then
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotalAmount().getAmount().intValue()).isEqualTo(18998);
    }

    @Test
    void should_throwException_when_addNullItem() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));

        // When & Then
        assertThatThrownBy(() -> order.addItem(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单项不能为空");
    }

    @Test
    void should_payOrder_when_created() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));

        // When
        order.pay();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.isPaid()).isTrue();
    }

    @Test
    void should_throwException_when_payWithoutItems() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));

        // When & Then
        assertThatThrownBy(() -> order.pay())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单项不能为空");
    }

    @Test
    void should_throwException_when_payPaidOrder() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();

        // When & Then
        assertThatThrownBy(() -> order.pay())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许支付");
    }

    @Test
    void should_shipOrder_when_paid() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();

        // When
        order.ship();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void should_throwException_when_shipCreatedOrder() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));

        // When & Then
        assertThatThrownBy(() -> order.ship())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许发货");
    }

    @Test
    void should_deliverOrder_when_shipped() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();
        order.ship();

        // When
        order.deliver();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.isDelivered()).isTrue();
    }

    @Test
    void should_throwException_when_deliverPaidOrder() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();

        // When & Then
        assertThatThrownBy(() -> order.deliver())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许确认送达");
    }

    @Test
    void should_cancelOrder_when_created() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));

        // When
        order.cancel();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.isCancelled()).isTrue();
    }

    @Test
    void should_cancelOrder_when_paid() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();

        // When
        order.cancel();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void should_throwException_when_cancelShippedOrder() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();
        order.ship();

        // When & Then
        assertThatThrownBy(() -> order.cancel())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许取消");
    }

    @Test
    void should_reconstructOrder() {
        // Given
        OrderId orderId = new OrderId("ORD123456");
        CustomerId customerId = new CustomerId("CUST001");
        OrderItem item = OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999));

        // When
        Order order = Order.reconstruct(
                1L, orderId, customerId, OrderStatus.PAID,
                Arrays.asList(item), Money.cny(5999),
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        // Then
        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getOrderIdValue()).isEqualTo("ORD123456");
        assertThat(order.getCustomerIdValue()).isEqualTo("CUST001");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void should_updateUpdateTime_when_stateChanges() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        java.time.LocalDateTime beforePay = order.getUpdateTime();

        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }
        order.pay();

        // Then
        assertThat(order.getUpdateTime()).isAfter(beforePay);
    }

    @Test
    void should_returnUnmodifiableItemsList() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));

        // When
        java.util.List<OrderItem> items = order.getItems();

        // Then
        assertThatThrownBy(() -> items.add(OrderItem.create("PROD002", "Mac", 1, Money.cny(100))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_setId() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));

        // When
        order.setId(123L);

        // Then
        assertThat(order.getId()).isEqualTo(123L);
    }

    // --- Coupon related tests ---

    @Test
    void should_applyCouponAndCalculateFinalAmount_when_validCoupon() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(100)));

        // When
        order.applyCoupon(new CouponId("COUPON001"), Money.cny(20));

        // Then
        assertThat(order.getCouponId()).isNotNull();
        assertThat(order.getCouponIdValue()).isEqualTo("COUPON001");
        assertThat(order.getDiscountAmount()).isEqualTo(Money.cny(20));
        assertThat(order.getFinalAmount()).isEqualTo(Money.cny(80));
    }

    @Test
    void should_calculateFinalAmount_when_noCoupon() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(100)));

        // Then
        assertThat(order.getCouponId()).isNull();
        assertThat(order.getDiscountAmount()).isEqualTo(Money.cny(0));
        assertThat(order.getFinalAmount()).isEqualTo(Money.cny(100));
    }

    @Test
    void should_throwException_when_applyCouponWithNullId() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(100)));

        // When & Then
        assertThatThrownBy(() -> order.applyCoupon(null, Money.cny(20)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("优惠券ID不能为空");
    }

    @Test
    void should_throwException_when_applyCouponWithNullDiscount() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(100)));

        // When & Then
        assertThatThrownBy(() -> order.applyCoupon(new CouponId("COUPON001"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("折扣金额不能为空");
    }

    @Test
    void should_throwException_when_applyCouponWithZeroDiscount() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(100)));

        // When & Then
        assertThatThrownBy(() -> order.applyCoupon(new CouponId("COUPON001"), Money.cny(0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("折扣金额必须大于0");
    }

    @Test
    void should_throwException_when_discountExceedsTotal() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(50)));

        // When & Then
        assertThatThrownBy(() -> order.applyCoupon(new CouponId("COUPON001"), Money.cny(60)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("折扣金额不能大于订单金额");
    }

    @Test
    void should_removeCouponAndResetAmounts_when_couponApplied() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(100)));
        order.applyCoupon(new CouponId("COUPON001"), Money.cny(20));

        // When
        order.removeCoupon();

        // Then
        assertThat(order.getCouponId()).isNull();
        assertThat(order.getDiscountAmount()).isEqualTo(Money.cny(0));
        assertThat(order.getFinalAmount()).isEqualTo(Money.cny(100));
    }

    @Test
    void should_doNothing_when_removeCouponButNoCouponApplied() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(100)));

        // When
        order.removeCoupon();

        // Then
        assertThat(order.getCouponId()).isNull();
        assertThat(order.getDiscountAmount()).isEqualTo(Money.cny(0));
        assertThat(order.getFinalAmount()).isEqualTo(Money.cny(100));
    }

    // --- Refund related tests ---

    @Test
    void should_refundOrder_when_paid() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();

        // When
        order.refund();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.isRefunded()).isTrue();
    }

    @Test
    void should_refundOrder_when_shipped() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();
        order.ship();

        // When
        order.refund();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.isRefunded()).isTrue();
    }

    @Test
    void should_refundOrder_when_delivered() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.pay();
        order.ship();
        order.deliver();

        // When
        order.refund();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.isRefunded()).isTrue();
    }

    @Test
    void should_throwException_when_refundCreatedOrder() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));

        // When & Then
        assertThatThrownBy(() -> order.refund())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许退款");
    }

    @Test
    void should_throwException_when_refundCancelledOrder() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        order.cancel();

        // When & Then
        assertThatThrownBy(() -> order.refund())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许退款");
    }

    @Test
    void should_reconstructOrder_withCouponFields() {
        // Given
        OrderId orderId = new OrderId("ORD123456");
        CustomerId customerId = new CustomerId("CUST001");
        OrderItem item = OrderItem.create("PROD001", "iPhone", 1, Money.cny(100));
        CouponId couponId = new CouponId("COUPON001");
        Money discountAmount = Money.cny(20);
        Money finalAmount = Money.cny(80);

        // When
        Order order = Order.reconstruct(
                1L, orderId, customerId, OrderStatus.CREATED,
                Arrays.asList(item), Money.cny(100), discountAmount, finalAmount,
                couponId,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        // Then
        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getOrderIdValue()).isEqualTo("ORD123456");
        assertThat(order.getCouponId()).isEqualTo(couponId);
        assertThat(order.getCouponIdValue()).isEqualTo("COUPON001");
        assertThat(order.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(order.getFinalAmount()).isEqualTo(finalAmount);
    }
}
