package com.claudej.domain.order.model.entity;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.order.model.valobj.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 订单项实体 - 属于 Order 聚合
 */
@Getter
@EqualsAndHashCode
@ToString
public class OrderItem {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;
    private final Money subtotal;

    private OrderItem(String productId, String productName, int quantity, Money unitPrice, Money subtotal) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    /**
     * 工厂方法：创建订单项
     */
    public static OrderItem create(String productId, String productName, int quantity, Money unitPrice) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "商品ID不能为空");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "商品名称不能为空");
        }
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_QUANTITY_INVALID);
        }
        if (unitPrice == null) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_PRICE_INVALID);
        }

        Money subtotal = unitPrice.multiply(quantity);
        return new OrderItem(productId.trim(), productName.trim(), quantity, unitPrice, subtotal);
    }

    /**
     * 从持久化层重建实体
     */
    public static OrderItem reconstruct(String productId, String productName, int quantity,
                                         Money unitPrice, Money subtotal) {
        return new OrderItem(productId, productName, quantity, unitPrice, subtotal);
    }
}
