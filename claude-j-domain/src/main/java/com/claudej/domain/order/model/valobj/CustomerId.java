package com.claudej.domain.order.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 客户ID值对象
 */
@Getter
@EqualsAndHashCode
@ToString
public final class CustomerId {

    private final String value;

    public CustomerId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "客户ID不能为空");
        }
        this.value = value.trim();
    }
}
