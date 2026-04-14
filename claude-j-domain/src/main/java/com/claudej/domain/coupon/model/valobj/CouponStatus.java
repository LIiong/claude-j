package com.claudej.domain.coupon.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 优惠券状态枚举 - 封装状态转换规则
 * AVAILABLE -> USED（使用）
 * AVAILABLE -> EXPIRED（过期）
 * 其他转换抛 BusinessException
 */
@Getter
public enum CouponStatus {

    AVAILABLE("可用"),
    USED("已使用"),
    EXPIRED("已过期");

    private final String description;

    CouponStatus(String description) {
        this.description = description;
    }

    /**
     * 是否可以使用
     */
    public boolean canUse() {
        return this == AVAILABLE;
    }

    /**
     * 是否可以过期
     */
    public boolean canExpire() {
        return this == AVAILABLE;
    }

    /**
     * 转换到已使用状态
     */
    public CouponStatus toUsed() {
        if (!canUse()) {
            throw new BusinessException(ErrorCode.INVALID_COUPON_STATUS_TRANSITION,
                    "优惠券状态 " + this + " 不允许使用");
        }
        return USED;
    }

    /**
     * 转换到已过期状态
     */
    public CouponStatus toExpired() {
        if (!canExpire()) {
            throw new BusinessException(ErrorCode.INVALID_COUPON_STATUS_TRANSITION,
                    "优惠券状态 " + this + " 不允许过期");
        }
        return EXPIRED;
    }
}
