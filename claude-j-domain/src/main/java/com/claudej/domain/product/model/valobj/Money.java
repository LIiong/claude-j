package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额值对象 - 不可变（聚合内独立）
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Money {

    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_NEGATIVE, "金额不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_NEGATIVE);
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_ZERO);
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
}