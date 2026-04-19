package com.claudej.domain.order.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额值对象 - 不可变
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Money {

    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.MONEY_AMOUNT_NEGATIVE, "金额不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.MONEY_AMOUNT_NEGATIVE);
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MONEY_CURRENCY_EMPTY);
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.trim().toUpperCase();
    }

    /**
     * 便捷构造方法（CNY 币种）
     */
    public static Money cny(BigDecimal amount) {
        return new Money(amount, "CNY");
    }

    /**
     * 便捷构造方法（CNY 币种）
     */
    public static Money cny(double amount) {
        return new Money(BigDecimal.valueOf(amount), "CNY");
    }

    /**
     * 金额相加
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION, "币种不一致，无法相加");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 金额相减（确保结果不为负）n     */
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new BusinessException(ErrorCode.CART_CURRENCY_MISMATCH, "币种不一致，无法相减");
        }
        BigDecimal result = this.amount.subtract(other.amount);
        return new Money(result, this.currency);
    }

    /**
     * 金额相乘
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    /**
     * 金额比较
     */
    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * 金额是否为零
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
