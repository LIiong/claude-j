package com.claudej.domain.inventory.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * SKU编码值对象 - 商品库存编码标识
 */
@Getter
@EqualsAndHashCode
@ToString
public final class SkuCode {

    private static final int MAX_LENGTH = 32;

    private final String value;

    public SkuCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SKU_CODE_EMPTY, "SKU编码不能为空");
        }
        String trimmedValue = value.trim();
        if (trimmedValue.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SKU_CODE_TOO_LONG, "SKU编码长度不能超过" + MAX_LENGTH);
        }
        this.value = trimmedValue;
    }
}