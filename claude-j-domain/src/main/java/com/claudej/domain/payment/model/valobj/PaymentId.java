package com.claudej.domain.payment.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 支付ID值对象 - 业务支付标识
 */
@Getter
@EqualsAndHashCode
@ToString
public final class PaymentId {

    private final String value;

    public PaymentId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_ID_EMPTY, "支付ID不能为空");
        }
        this.value = value.trim();
    }
}