package com.claudej.infrastructure.cart.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.repository.CartRepository;
import com.claudej.infrastructure.cart.persistence.converter.CartConverter;
import com.claudej.infrastructure.cart.persistence.dataobject.CartDO;
import com.claudej.infrastructure.cart.persistence.dataobject.CartItemDO;
import com.claudej.infrastructure.cart.persistence.mapper.CartItemMapper;
import com.claudej.infrastructure.cart.persistence.mapper.CartMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 购物车 Repository 实现
 */
@Repository
public class CartRepositoryImpl implements CartRepository {

    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final CartConverter cartConverter;

    public CartRepositoryImpl(CartMapper cartMapper, CartItemMapper cartItemMapper,
                              CartConverter cartConverter) {
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.cartConverter = cartConverter;
    }

    @Override
    @Transactional
    public Cart save(Cart cart) {
        if (cart.getId() == null) {
            // 新增购物车
            CartDO cartDO = cartConverter.toDO(cart);
            cartDO.setCreateTime(LocalDateTime.now());
            cartDO.setUpdateTime(LocalDateTime.now());
            cartDO.setDeleted(0);
            cartMapper.insert(cartDO);

            // 插入购物车项
            List<CartItemDO> itemDOList = cartConverter.toItemDOList(cart.getItems(), cartDO.getId());
            for (CartItemDO itemDO : itemDOList) {
                itemDO.setCreateTime(LocalDateTime.now());
                itemDO.setUpdateTime(LocalDateTime.now());
                itemDO.setDeleted(0);
                cartItemMapper.insert(itemDO);
            }

            // 重新查询并返回
            return findByUserId(cart.getUserId()).orElse(null);
        } else {
            // 更新购物车
            CartDO cartDO = cartConverter.toDO(cart);
            cartDO.setUpdateTime(LocalDateTime.now());
            cartMapper.updateById(cartDO);

            // 删除旧购物车项，插入新购物车项
            LambdaQueryWrapper<CartItemDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CartItemDO::getCartId, cart.getId());
            cartItemMapper.delete(wrapper);

            List<CartItemDO> itemDOList = cartConverter.toItemDOList(cart.getItems(), cart.getId());
            for (CartItemDO itemDO : itemDOList) {
                itemDO.setCreateTime(LocalDateTime.now());
                itemDO.setUpdateTime(LocalDateTime.now());
                itemDO.setDeleted(0);
                cartItemMapper.insert(itemDO);
            }

            // 重新查询并返回
            return findByUserId(cart.getUserId()).orElse(null);
        }
    }

    @Override
    public Optional<Cart> findByUserId(String userId) {
        LambdaQueryWrapper<CartDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartDO::getUserId, userId);
        CartDO cartDO = cartMapper.selectOne(wrapper);

        if (cartDO == null) {
            return Optional.empty();
        }

        // 查询购物车项
        LambdaQueryWrapper<CartItemDO> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(CartItemDO::getCartId, cartDO.getId());
        List<CartItemDO> itemDOList = cartItemMapper.selectList(itemWrapper);

        Cart cart = cartConverter.toDomain(cartDO, itemDOList);
        return Optional.ofNullable(cart);
    }

    @Override
    @Transactional
    public void deleteByUserId(String userId) {
        LambdaQueryWrapper<CartDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartDO::getUserId, userId);
        CartDO cartDO = cartMapper.selectOne(wrapper);

        if (cartDO != null) {
            // 删除购物车项
            LambdaQueryWrapper<CartItemDO> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(CartItemDO::getCartId, cartDO.getId());
            cartItemMapper.delete(itemWrapper);

            // 删除购物车
            cartMapper.deleteById(cartDO.getId());
        }
    }
}
