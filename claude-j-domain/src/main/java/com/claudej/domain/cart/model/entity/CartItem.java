package com.claudej.domain.cart.model.entity;

import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import lombok.Getter;

/**
 * 购物车项实体 - 聚合内实体
 */
@Getter
public class CartItem {

    private String productId;
    private String productName;
    private Money unitPrice;
    private Quantity quantity;
    private Money subtotal;

    /**
     * 私有构造方法，通过工厂方法创建
     */
    private CartItem() {
    }

    /**
     * 工厂方法：创建购物车项
     */
    public static CartItem create(String productId, String productName, Money unitPrice, Quantity quantity) {
        CartItem item = new CartItem();
        item.productId = productId;
        item.productName = productName;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.calculateSubtotal();
        return item;
    }

    /**
     * 重建方法：从持久化恢复（不执行业务校验）
     */
    public static CartItem reconstruct(String productId, String productName, Money unitPrice, 
                                       Quantity quantity, Money subtotal) {
        CartItem item = new CartItem();
        item.productId = productId;
        item.productName = productName;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.subtotal = subtotal;
        return item;
    }

    /**
     * 更新数量
     */
    public void updateQuantity(Quantity newQuantity) {
        this.quantity = newQuantity;
        calculateSubtotal();
    }

    /**
     * 增加数量
     */
    public void increaseQuantity(Quantity additionalQuantity) {
        this.quantity = this.quantity.add(additionalQuantity);
        calculateSubtotal();
    }

    /**
     * 计算小计金额
     */
    private void calculateSubtotal() {
        this.subtotal = unitPrice.multiply(quantity.getValue());
    }
}
