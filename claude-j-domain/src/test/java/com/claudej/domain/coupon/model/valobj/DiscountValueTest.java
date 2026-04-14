package com.claudej.domain.coupon.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountValueTest {

    @Test
    void should_createFixedAmount_when_positiveValue() {
        // When
        DiscountValue dv = new DiscountValue(new BigDecimal("20.00"), DiscountType.FIXED_AMOUNT);

        // Then
        assertThat(dv.getValue()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(dv.getType()).isEqualTo(DiscountType.FIXED_AMOUNT);
    }

    @Test
    void should_throwException_when_fixedAmountIsZero() {
        assertThatThrownBy(() -> new DiscountValue(BigDecimal.ZERO, DiscountType.FIXED_AMOUNT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("固定金额折扣值必须大于0");
    }

    @Test
    void should_throwException_when_fixedAmountIsNegative() {
        assertThatThrownBy(() -> new DiscountValue(new BigDecimal("-10"), DiscountType.FIXED_AMOUNT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("固定金额折扣值必须大于0");
    }

    @Test
    void should_createPercentage_when_validIntegerValue() {
        // When
        DiscountValue dv = new DiscountValue(new BigDecimal("50"), DiscountType.PERCENTAGE);

        // Then
        assertThat(dv.getValue()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(dv.getType()).isEqualTo(DiscountType.PERCENTAGE);
    }

    @Test
    void should_createPercentage_when_boundaryValue1() {
        DiscountValue dv = new DiscountValue(BigDecimal.ONE, DiscountType.PERCENTAGE);
        assertThat(dv.getValue()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void should_createPercentage_when_boundaryValue100() {
        DiscountValue dv = new DiscountValue(new BigDecimal("100"), DiscountType.PERCENTAGE);
        assertThat(dv.getValue()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void should_throwException_when_percentageLessThan1() {
        assertThatThrownBy(() -> new DiscountValue(BigDecimal.ZERO, DiscountType.PERCENTAGE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("百分比折扣值必须在1-100之间");
    }

    @Test
    void should_throwException_when_percentageGreaterThan100() {
        assertThatThrownBy(() -> new DiscountValue(new BigDecimal("101"), DiscountType.PERCENTAGE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("百分比折扣值必须在1-100之间");
    }

    @Test
    void should_throwException_when_percentageIsNotInteger() {
        assertThatThrownBy(() -> new DiscountValue(new BigDecimal("50.5"), DiscountType.PERCENTAGE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("百分比折扣值必须为整数");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        assertThatThrownBy(() -> new DiscountValue(null, DiscountType.FIXED_AMOUNT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("折扣值不能为空");
    }

    @Test
    void should_throwException_when_typeIsNull() {
        assertThatThrownBy(() -> new DiscountValue(new BigDecimal("20"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("折扣类型不能为空");
    }

    @Test
    void should_beEqual_when_sameValueAndType() {
        DiscountValue dv1 = new DiscountValue(new BigDecimal("20.00"), DiscountType.FIXED_AMOUNT);
        DiscountValue dv2 = new DiscountValue(new BigDecimal("20.00"), DiscountType.FIXED_AMOUNT);
        assertThat(dv1).isEqualTo(dv2);
        assertThat(dv1.hashCode()).isEqualTo(dv2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentType() {
        DiscountValue dv1 = new DiscountValue(new BigDecimal("20"), DiscountType.FIXED_AMOUNT);
        DiscountValue dv2 = new DiscountValue(new BigDecimal("20"), DiscountType.PERCENTAGE);
        assertThat(dv1).isNotEqualTo(dv2);
    }
}
