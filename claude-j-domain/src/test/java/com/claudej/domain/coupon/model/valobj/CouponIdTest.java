package com.claudej.domain.coupon.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponIdTest {

    @Test
    void should_createCouponId_when_validValue() {
        // When
        CouponId couponId = new CouponId("abc123");

        // Then
        assertThat(couponId.getValue()).isEqualTo("abc123");
    }

    @Test
    void should_trimValue_when_valueHasWhitespace() {
        // When
        CouponId couponId = new CouponId("  abc123  ");

        // Then
        assertThat(couponId.getValue()).isEqualTo("abc123");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        assertThatThrownBy(() -> new CouponId(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        assertThatThrownBy(() -> new CouponId(""))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throwException_when_valueIsBlank() {
        assertThatThrownBy(() -> new CouponId("   "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Given
        CouponId id1 = new CouponId("abc123");
        CouponId id2 = new CouponId("abc123");

        // Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Given
        CouponId id1 = new CouponId("abc123");
        CouponId id2 = new CouponId("def456");

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }
}
