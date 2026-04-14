package com.claudej.domain.order.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    @Test
    void should_allowPay_when_created() {
        assertThat(OrderStatus.CREATED.canPay()).isTrue();
        assertThat(OrderStatus.PAID.canPay()).isFalse();
        assertThat(OrderStatus.SHIPPED.canPay()).isFalse();
        assertThat(OrderStatus.DELIVERED.canPay()).isFalse();
        assertThat(OrderStatus.CANCELLED.canPay()).isFalse();
    }

    @Test
    void should_allowShip_when_paid() {
        assertThat(OrderStatus.CREATED.canShip()).isFalse();
        assertThat(OrderStatus.PAID.canShip()).isTrue();
        assertThat(OrderStatus.SHIPPED.canShip()).isFalse();
        assertThat(OrderStatus.DELIVERED.canShip()).isFalse();
        assertThat(OrderStatus.CANCELLED.canShip()).isFalse();
    }

    @Test
    void should_allowDeliver_when_shipped() {
        assertThat(OrderStatus.CREATED.canDeliver()).isFalse();
        assertThat(OrderStatus.PAID.canDeliver()).isFalse();
        assertThat(OrderStatus.SHIPPED.canDeliver()).isTrue();
        assertThat(OrderStatus.DELIVERED.canDeliver()).isFalse();
        assertThat(OrderStatus.CANCELLED.canDeliver()).isFalse();
    }

    @Test
    void should_allowCancel_when_createdOrPaid() {
        assertThat(OrderStatus.CREATED.canCancel()).isTrue();
        assertThat(OrderStatus.PAID.canCancel()).isTrue();
        assertThat(OrderStatus.SHIPPED.canCancel()).isFalse();
        assertThat(OrderStatus.DELIVERED.canCancel()).isFalse();
        assertThat(OrderStatus.CANCELLED.canCancel()).isFalse();
    }

    @Test
    void should_transitionToPaid_when_created() {
        // When
        OrderStatus newStatus = OrderStatus.CREATED.toPaid();

        // Then
        assertThat(newStatus).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void should_throwException_when_payFromPaid() {
        // When & Then
        assertThatThrownBy(() -> OrderStatus.PAID.toPaid())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许支付");
    }

    @Test
    void should_transitionToShipped_when_paid() {
        // When
        OrderStatus newStatus = OrderStatus.PAID.toShipped();

        // Then
        assertThat(newStatus).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void should_throwException_when_shipFromCreated() {
        // When & Then
        assertThatThrownBy(() -> OrderStatus.CREATED.toShipped())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许发货");
    }

    @Test
    void should_transitionToDelivered_when_shipped() {
        // When
        OrderStatus newStatus = OrderStatus.SHIPPED.toDelivered();

        // Then
        assertThat(newStatus).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void should_throwException_when_deliverFromPaid() {
        // When & Then
        assertThatThrownBy(() -> OrderStatus.PAID.toDelivered())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许确认送达");
    }

    @Test
    void should_transitionToCancelled_when_created() {
        // When
        OrderStatus newStatus = OrderStatus.CREATED.toCancelled();

        // Then
        assertThat(newStatus).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void should_transitionToCancelled_when_paid() {
        // When
        OrderStatus newStatus = OrderStatus.PAID.toCancelled();

        // Then
        assertThat(newStatus).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void should_throwException_when_cancelFromShipped() {
        // When & Then
        assertThatThrownBy(() -> OrderStatus.SHIPPED.toCancelled())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许取消");
    }

    @Test
    void should_throwException_when_cancelFromDelivered() {
        // When & Then
        assertThatThrownBy(() -> OrderStatus.DELIVERED.toCancelled())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许取消");
    }

    @Test
    void should_throwException_when_cancelFromCancelled() {
        // When & Then
        assertThatThrownBy(() -> OrderStatus.CANCELLED.toCancelled())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许取消");
    }
}
