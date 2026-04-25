package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 商品状态枚举 - 封装状态转换规则
 */
@Getter
public enum ProductStatus {

    DRAFT("草稿"),
    ACTIVE("已上架"),
    INACTIVE("已下架");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    /**
     * 是否可以上架
     */
    public boolean canActivate() {
        return this == DRAFT || this == INACTIVE;
    }

    /**
     * 是否可以下架
     */
    public boolean canDeactivate() {
        return this == ACTIVE;
    }

    /**
     * 转换到上架状态
     */
    public ProductStatus toActive() {
        if (!canActivate()) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_STATUS_TRANSITION,
                    "商品状态 " + this + " 不允许上架");
        }
        return ACTIVE;
    }

    /**
     * 转换到下架状态
     */
    public ProductStatus toInactive() {
        if (!canDeactivate()) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_STATUS_TRANSITION,
                    "商品状态 " + this + " 不允许下架");
        }
        return INACTIVE;
    }
}