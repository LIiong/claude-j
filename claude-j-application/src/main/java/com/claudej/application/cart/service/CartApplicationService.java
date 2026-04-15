package com.claudej.application.cart.service;

import com.claudej.application.cart.assembler.CartAssembler;
import com.claudej.application.cart.command.AddCartItemCommand;
import com.claudej.application.cart.command.UpdateCartItemQuantityCommand;
import com.claudej.application.cart.dto.CartDTO;
import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import com.claudej.domain.cart.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 购物车应用服务
 */
@Service
public class CartApplicationService {

    private final CartRepository cartRepository;
    private final CartAssembler cartAssembler;

    public CartApplicationService(CartRepository cartRepository, CartAssembler cartAssembler) {
        this.cartRepository = cartRepository;
        this.cartAssembler = cartAssembler;
    }

    /**
     * 添加商品到购物车
     */
    @Transactional
    public CartDTO addItem(AddCartItemCommand command) {
        Cart cart = cartRepository.findByUserId(command.getUserId())
            .orElse(Cart.create(command.getUserId()));

        cart.addItem(
            command.getProductId(),
            command.getProductName(),
            Money.cny(command.getUnitPrice()),
            new Quantity(command.getQuantity())
        );

        Cart savedCart = cartRepository.save(cart);
        return cartAssembler.toDTO(savedCart);
    }

    /**
     * 更新商品数量
     */
    @Transactional
    public CartDTO updateItemQuantity(UpdateCartItemQuantityCommand command) {
        Cart cart = cartRepository.findByUserId(command.getUserId())
            .orElseThrow(() -> new com.claudej.domain.common.exception.BusinessException(
                com.claudej.domain.common.exception.ErrorCode.CART_NOT_FOUND));

        cart.updateItemQuantity(command.getProductId(), new Quantity(command.getQuantity()));

        Cart savedCart = cartRepository.save(cart);
        return cartAssembler.toDTO(savedCart);
    }

    /**
     * 删除购物车商品
     */
    @Transactional
    public CartDTO removeItem(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new com.claudej.domain.common.exception.BusinessException(
                com.claudej.domain.common.exception.ErrorCode.CART_NOT_FOUND));

        cart.removeItem(productId);

        Cart savedCart = cartRepository.save(cart);
        return cartAssembler.toDTO(savedCart);
    }

    /**
     * 清空购物车
     */
    @Transactional
    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new com.claudej.domain.common.exception.BusinessException(
                com.claudej.domain.common.exception.ErrorCode.CART_NOT_FOUND));

        cart.clear();
        cartRepository.save(cart);
    }

    /**
     * 查询购物车
     */
    @Transactional(readOnly = true)
    public CartDTO getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElse(Cart.create(userId));
        return cartAssembler.toDTO(cart);
    }
}
