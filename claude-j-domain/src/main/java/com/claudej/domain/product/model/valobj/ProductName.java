package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 商品名称值对象 - 商品名称信息
 */
@Getter
@EqualsAndHashCode
@ToString
public final class ProductName {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    private final String value;

    public ProductName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_EMPTY);
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_LENGTH_INVALID);
        }
        this.value = trimmed;
    }
}