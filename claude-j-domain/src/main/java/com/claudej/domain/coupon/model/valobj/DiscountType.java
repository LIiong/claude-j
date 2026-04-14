package com.claudej.domain.coupon.model.valobj;

import lombok.Getter;

/**
 * 折扣类型枚举
 */
@Getter
public enum DiscountType {

    FIXED_AMOUNT("固定金额"),
    PERCENTAGE("百分比");

    private final String description;

    DiscountType(String description) {
        this.description = description;
    }
}
