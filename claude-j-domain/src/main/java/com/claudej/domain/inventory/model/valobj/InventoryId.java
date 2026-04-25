package com.claudej.domain.inventory.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 库存ID值对象 - 库存唯一业务标识
 */
@Getter
@EqualsAndHashCode
@ToString
public final class InventoryId {

    private final String value;

    public InventoryId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVENTORY_ID_EMPTY, "库存ID不能为空");
        }
        this.value = value.trim();
    }
}