package com.claudej.domain.cart.model.aggregate;

import com.claudej.domain.cart.model.entity.CartItem;
import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 购物车聚合根
 */
@Getter
public class Cart {

    private Long id;
    private String userId;
    private List<CartItem> items;
    private Money totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 私有构造方法
     */
    private Cart() {
        this.items = new ArrayList<>();
        this.totalAmount = Money.cny(0);
    }

    /**
     * 工厂方法：创建新购物车
     */
    public static Cart create(String userId) {
        Cart cart = new Cart();
        cart.userId = userId;
        cart.createTime = LocalDateTime.now();
        cart.updateTime = cart.createTime;
        return cart;
    }

    /**
     * 重建方法：从持久化恢复
     */
    public static Cart reconstruct(Long id, String userId, List<CartItem> items, 
                                   Money totalAmount, LocalDateTime createTime, LocalDateTime updateTime) {
        Cart cart = new Cart();
        cart.id = id;
        cart.userId = userId;
        cart.items = new ArrayList<>(items != null ? items : Collections.emptyList());
        cart.totalAmount = totalAmount;
        cart.createTime = createTime;
        cart.updateTime = updateTime;
        return cart;
    }

    /**
     * 添加商品到购物车
     * 如果商品已存在，则累加数量
     */
    public void addItem(String productId, String productName, Money unitPrice, Quantity quantity) {
        Optional<CartItem> existingItem = findItemByProductId(productId);
        
        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(quantity);
        } else {
            CartItem newItem = CartItem.create(productId, productName, unitPrice, quantity);
            items.add(newItem);
        }
        
        recalculateTotalAmount();
        updateTime = LocalDateTime.now();
    }

    /**
     * 更新商品数量
     */
    public void updateItemQuantity(String productId, Quantity newQuantity) {
        CartItem item = findItemByProductId(productId)
            .orElseThrow(() -> new com.claudej.domain.common.exception.BusinessException(
                com.claudej.domain.common.exception.ErrorCode.CART_ITEM_NOT_FOUND, "购物车中不存在该商品"));
        
        item.updateQuantity(newQuantity);
        recalculateTotalAmount();
        updateTime = LocalDateTime.now();
    }

    /**
     * 删除商品
     */
    public void removeItem(String productId) {
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new com.claudej.domain.common.exception.BusinessException(
                com.claudej.domain.common.exception.ErrorCode.CART_ITEM_NOT_FOUND, "购物车中不存在该商品");
        }
        recalculateTotalAmount();
        updateTime = LocalDateTime.now();
    }

    /**
     * 清空购物车
     */
    public void clear() {
        items.clear();
        totalAmount = Money.cny(0);
        updateTime = LocalDateTime.now();
    }

    /**
     * 获取商品数量
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * 是否为空购物车
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * 根据商品ID查找购物车项
     */
    private Optional<CartItem> findItemByProductId(String productId) {
        return items.stream()
            .filter(item -> item.getProductId().equals(productId))
            .findFirst();
    }

    /**
     * 重新计算总金额
     */
    private void recalculateTotalAmount() {
        totalAmount = items.stream()
            .map(CartItem::getSubtotal)
            .reduce(Money.cny(0), Money::add);
    }
}
