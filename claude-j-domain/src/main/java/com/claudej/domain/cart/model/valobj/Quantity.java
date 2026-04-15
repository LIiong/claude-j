package com.claudej.domain.cart.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 数量值对象 - 不可变
 * 约束：1 <= quantity <= 999
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Quantity {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 999;

    private final int value;

    public Quantity(int value) {
        if (value < MIN_QUANTITY) {
            throw new BusinessException(ErrorCode.CART_ITEM_QUANTITY_INVALID, 
                "商品数量不能小于" + MIN_QUANTITY);
        }
        if (value > MAX_QUANTITY) {
            throw new BusinessException(ErrorCode.CART_ITEM_QUANTITY_INVALID, 
                "商品数量不能超过" + MAX_QUANTITY);
        }
        this.value = value;
    }

    /**
     * 增加数量
     */
    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    /**
     * 修改数量
     */
    public Quantity update(int newValue) {
        return new Quantity(newValue);
    }
}
