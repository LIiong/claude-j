package com.claudej.domain.coupon.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.coupon.model.valobj.CouponStatus;
import com.claudej.domain.coupon.model.valobj.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 14, 12, 0, 0);
    private static final LocalDateTime VALID_FROM = LocalDateTime.of(2026, 4, 1, 0, 0, 0);
    private static final LocalDateTime VALID_UNTIL = LocalDateTime.of(2026, 5, 1, 23, 59, 59);

    @Test
    void should_createCoupon_when_validFixedAmountParams() {
        // When
        Coupon coupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "user123",
                VALID_FROM, VALID_UNTIL);

        // Then
        assertThat(coupon.getCouponIdValue()).isNotEmpty();
        assertThat(coupon.getName()).isEqualTo("满100减20");
        assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
        assertThat(coupon.getDiscountValue().getValue()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(coupon.getMinOrderAmount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(coupon.getCurrency()).isEqualTo("CNY");
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
        assertThat(coupon.getUserId()).isEqualTo("user123");
    }

    @Test
    void should_createCoupon_when_validPercentageParams() {
        // When
        Coupon coupon = Coupon.create("8折", DiscountType.PERCENTAGE,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        // Then
        assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(coupon.getDiscountValue().getValue()).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void should_defaultMinOrderAmountToZero_when_null() {
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("10"), null, "user123",
                VALID_FROM, VALID_UNTIL);
        assertThat(coupon.getMinOrderAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_throwException_when_nameIsEmpty() {
        assertThatThrownBy(() -> Coupon.create("", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("名称不能为空");
    }

    @Test
    void should_throwException_when_nameIsNull() {
        assertThatThrownBy(() -> Coupon.create(null, DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("名称不能为空");
    }

    @Test
    void should_throwException_when_nameTooLong() {
        String longName = "a]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]";
        assertThatThrownBy(() -> Coupon.create(longName, DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("长度不能超过50");
    }

    @Test
    void should_throwException_when_userIdIsEmpty() {
        assertThatThrownBy(() -> Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "",
                VALID_FROM, VALID_UNTIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID不能为空");
    }

    @Test
    void should_throwException_when_validFromAfterValidUntil() {
        assertThatThrownBy(() -> Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_UNTIL, VALID_FROM))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("有效期开始时间必须早于截止时间");
    }

    @Test
    void should_throwException_when_validFromEqualsValidUntil() {
        assertThatThrownBy(() -> Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_FROM))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("有效期开始时间必须早于截止时间");
    }

    @Test
    void should_throwException_when_minOrderAmountNegative() {
        assertThatThrownBy(() -> Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("-1"), "user123",
                VALID_FROM, VALID_UNTIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最低订单金额不能为负数");
    }

    // --- use() tests ---

    @Test
    void should_useCoupon_when_availableAndWithinValidity() {
        // Given
        Coupon coupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "user123",
                VALID_FROM, VALID_UNTIL);

        // When
        coupon.use("order456", NOW);

        // Then
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
        assertThat(coupon.getUsedOrderId()).isEqualTo("order456");
        assertThat(coupon.getUsedTime()).isEqualTo(NOW);
    }

    @Test
    void should_throwException_when_useExpiredCoupon() {
        // Given
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);
        LocalDateTime afterExpiry = VALID_UNTIL.plusDays(1);

        // When & Then
        assertThatThrownBy(() -> coupon.use("order456", afterExpiry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throwException_when_useNotYetValidCoupon() {
        // Given
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);
        LocalDateTime beforeValid = VALID_FROM.minusDays(1);

        // When & Then
        assertThatThrownBy(() -> coupon.use("order456", beforeValid))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未生效");
    }

    @Test
    void should_throwException_when_useWithEmptyOrderId() {
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        assertThatThrownBy(() -> coupon.use("", NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单号不能为空");
    }

    @Test
    void should_throwException_when_useWithNullOrderId() {
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        assertThatThrownBy(() -> coupon.use(null, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单号不能为空");
    }

    @Test
    void should_throwException_when_useAlreadyUsedCoupon() {
        // Given
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);
        coupon.use("order1", NOW);

        // When & Then
        assertThatThrownBy(() -> coupon.use("order2", NOW))
                .isInstanceOf(BusinessException.class);
    }

    // --- checkAndExpire() tests ---

    @Test
    void should_expire_when_pastValidUntil() {
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        boolean expired = coupon.checkAndExpire(VALID_UNTIL.plusDays(1));

        assertThat(expired).isTrue();
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
    }

    @Test
    void should_notExpire_when_withinValidity() {
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        boolean expired = coupon.checkAndExpire(NOW);

        assertThat(expired).isFalse();
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
    }

    @Test
    void should_notExpire_when_alreadyUsed() {
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);
        coupon.use("order1", NOW);

        boolean expired = coupon.checkAndExpire(VALID_UNTIL.plusDays(1));

        assertThat(expired).isFalse();
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
    }

    // --- calculateDiscount() tests ---

    @Test
    void should_returnFixedAmount_when_orderAmountIsLarger() {
        Coupon coupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "user123",
                VALID_FROM, VALID_UNTIL);

        BigDecimal discount = coupon.calculateDiscount(new BigDecimal("200"));
        assertThat(discount).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void should_returnOrderAmount_when_fixedAmountExceedsOrder() {
        Coupon coupon = Coupon.create("减50", DiscountType.FIXED_AMOUNT,
                new BigDecimal("50"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        BigDecimal discount = coupon.calculateDiscount(new BigDecimal("30"));
        assertThat(discount).isEqualByComparingTo(new BigDecimal("30"));
    }

    @Test
    void should_calculatePercentageDiscount_when_percentage() {
        Coupon coupon = Coupon.create("8折", DiscountType.PERCENTAGE,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        BigDecimal discount = coupon.calculateDiscount(new BigDecimal("100"));
        assertThat(discount).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void should_roundDown_when_percentageHasFractions() {
        Coupon coupon = Coupon.create("15%折扣", DiscountType.PERCENTAGE,
                new BigDecimal("15"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        BigDecimal discount = coupon.calculateDiscount(new BigDecimal("33.33"));
        // 33.33 * 15 / 100 = 4.9995 -> 向下取整到分 -> 4.99
        assertThat(discount).isEqualByComparingTo(new BigDecimal("4.99"));
    }

    @Test
    void should_returnZero_when_orderAmountIsNull() {
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        assertThat(coupon.calculateDiscount(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_returnZero_when_orderAmountIsZero() {
        Coupon coupon = Coupon.create("优惠券", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), BigDecimal.ZERO, "user123",
                VALID_FROM, VALID_UNTIL);

        assertThat(coupon.calculateDiscount(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
