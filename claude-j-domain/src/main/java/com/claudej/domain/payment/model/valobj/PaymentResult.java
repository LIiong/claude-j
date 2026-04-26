package com.claudej.domain.payment.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 支付结果值对象 - PSP 返回结果
 */
@Getter
@EqualsAndHashCode
@ToString
public final class PaymentResult {

    private final boolean success;
    private final String transactionNo;
    private final String message;

    private PaymentResult(boolean success, String transactionNo, String message) {
        this.success = success;
        this.transactionNo = transactionNo;
        this.message = message;
    }

    /**
     * 创建成功结果
     */
    public static PaymentResult success(String transactionNo) {
        if (transactionNo == null || transactionNo.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "交易号不能为空");
        }
        return new PaymentResult(true, transactionNo.trim(), "支付成功");
    }

    /**
     * 创建失败结果
     */
    public static PaymentResult failed(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "失败消息不能为空");
        }
        return new PaymentResult(false, null, message.trim());
    }
}