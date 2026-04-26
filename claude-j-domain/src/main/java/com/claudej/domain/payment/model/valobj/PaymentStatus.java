package com.claudej.domain.payment.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 支付状态枚举 - 封装状态转换规则
 *
 * 状态机：
 * PENDING -> SUCCESS (支付成功回调)
 * PENDING -> FAILED (支付失败回调)
 * SUCCESS -> REFUNDED (管理员退款)
 *
 * FAILED/REFUNDED 为终态，不可再转换
 */
@Getter
public enum PaymentStatus {

    PENDING("待支付"),
    SUCCESS("支付成功"),
    FAILED("支付失败"),
    REFUNDED("已退款");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    /**
     * 是否可以转为成功状态
     */
    public boolean canSuccess() {
        return this == PENDING;
    }

    /**
     * 是否可以转为失败状态
     */
    public boolean canFail() {
        return this == PENDING;
    }

    /**
     * 是否可以退款
     */
    public boolean canRefund() {
        return this == SUCCESS;
    }

    /**
     * 是否为终态
     */
    public boolean isTerminal() {
        return this == FAILED || this == REFUNDED;
    }

    /**
     * 转换到成功状态
     */
    public PaymentStatus toSuccess() {
        if (!canSuccess()) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION,
                    "支付状态 " + this + " 不允许转为成功");
        }
        return SUCCESS;
    }

    /**
     * 转换到失败状态
     */
    public PaymentStatus toFailed() {
        if (!canFail()) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION,
                    "支付状态 " + this + " 不允许转为失败");
        }
        return FAILED;
    }

    /**
     * 转换到退款状态
     */
    public PaymentStatus toRefunded() {
        if (!canRefund()) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION,
                    "支付状态 " + this + " 不允许退款");
        }
        return REFUNDED;
    }
}