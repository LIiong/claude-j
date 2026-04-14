package com.claudej.domain.coupon.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 优惠券唯一标识值对象
 */
@Getter
@EqualsAndHashCode
@ToString
public final class CouponId {

    private final String value;

    public CouponId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.COUPON_ID_EMPTY, "优惠券ID不能为空");
        }
        this.value = value.trim();
    }
}
