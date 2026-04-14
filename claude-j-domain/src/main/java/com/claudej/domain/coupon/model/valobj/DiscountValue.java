package com.claudej.domain.coupon.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 折扣值值对象 - 根据折扣类型进行不同校验
 */
@Getter
@EqualsAndHashCode
@ToString
public final class DiscountValue {

    private final BigDecimal value;
    private final DiscountType type;

    public DiscountValue(BigDecimal value, DiscountType type) {
        if (value == null) {
            throw new BusinessException(ErrorCode.COUPON_DISCOUNT_VALUE_INVALID, "折扣值不能为空");
        }
        if (type == null) {
            throw new BusinessException(ErrorCode.COUPON_DISCOUNT_VALUE_INVALID, "折扣类型不能为空");
        }
        if (type == DiscountType.FIXED_AMOUNT) {
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.COUPON_DISCOUNT_VALUE_INVALID,
                        "固定金额折扣值必须大于0");
            }
        } else if (type == DiscountType.PERCENTAGE) {
            if (value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException(ErrorCode.COUPON_DISCOUNT_VALUE_INVALID,
                        "百分比折扣值必须在1-100之间");
            }
            // 百分比必须为整数
            if (value.stripTrailingZeros().scale() > 0) {
                throw new BusinessException(ErrorCode.COUPON_DISCOUNT_VALUE_INVALID,
                        "百分比折扣值必须为整数");
            }
        }
        this.value = value;
        this.type = type;
    }
}
