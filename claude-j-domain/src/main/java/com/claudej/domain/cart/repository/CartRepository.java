package com.claudej.domain.cart.repository;

import com.claudej.domain.cart.model.aggregate.Cart;

import java.util.Optional;

/**
 * 购物车仓储端口
 */
public interface CartRepository {

    /**
     * 保存购物车（含购物车项）
     */
    Cart save(Cart cart);

    /**
     * 根据用户ID查找购物车
     */
    Optional<Cart> findByUserId(String userId);

    /**
     * 根据用户ID删除购物车
     */
    void deleteByUserId(String userId);
}
