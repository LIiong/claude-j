package com.claudej.domain.order.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 订单号值对象 - 业务订单标识
 */
@Getter
@EqualsAndHashCode
@ToString
public final class OrderId {

    private final String value;

    public OrderId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单号不能为空");
        }
        this.value = value.trim();
    }
}
