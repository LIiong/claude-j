package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 商品ID值对象 - 商品唯一业务标识
 */
@Getter
@EqualsAndHashCode
@ToString
public final class ProductId {

    private final String value;

    public ProductId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_ID_EMPTY);
        }
        this.value = value.trim();
    }
}