package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * SKU值对象 - SKU信息（嵌入聚合）
 */
@Getter
@EqualsAndHashCode
@ToString
public final class SKU {

    private static final int MAX_SKU_CODE_LENGTH = 32;

    private final String skuCode;
    private final int stock;

    public SKU(String skuCode, int stock) {
        if (skuCode == null || skuCode.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_CODE_EMPTY);
        }
        String trimmed = skuCode.trim();
        if (trimmed.length() > MAX_SKU_CODE_LENGTH) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_CODE_LENGTH_INVALID);
        }
        if (stock < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_NEGATIVE);
        }
        this.skuCode = trimmed;
        this.stock = stock;
    }
}