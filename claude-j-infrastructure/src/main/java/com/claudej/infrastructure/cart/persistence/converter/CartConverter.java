package com.claudej.infrastructure.cart.persistence.converter;

import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.model.entity.CartItem;
import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import com.claudej.infrastructure.cart.persistence.dataobject.CartDO;
import com.claudej.infrastructure.cart.persistence.dataobject.CartItemDO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cart 转换器
 */
@Component
public class CartConverter {

    /**
     * Cart DO 转 Domain
     */
    public Cart toDomain(CartDO cartDO, List<CartItemDO> itemDOList) {
        if (cartDO == null) {
            return null;
        }

        List<CartItem> items = itemDOList == null ? new ArrayList<>()
                : itemDOList.stream()
                        .map(this::toItemDomain)
                        .collect(Collectors.toList());

        Money totalAmount = new Money(cartDO.getTotalAmount(), cartDO.getCurrency());

        return Cart.reconstruct(
                cartDO.getId(),
                cartDO.getUserId(),
                items,
                totalAmount,
                cartDO.getCreateTime(),
                cartDO.getUpdateTime()
        );
    }

    /**
     * Cart Domain 转 DO
     */
    public CartDO toDO(Cart cart) {
        if (cart == null) {
            return null;
        }
        CartDO cartDO = new CartDO();
        cartDO.setId(cart.getId());
        cartDO.setUserId(cart.getUserId());
        cartDO.setTotalAmount(cart.getTotalAmount().getAmount());
        cartDO.setCurrency(cart.getTotalAmount().getCurrency());
        cartDO.setCreateTime(cart.getCreateTime());
        cartDO.setUpdateTime(cart.getUpdateTime());
        cartDO.setDeleted(0);
        return cartDO;
    }

    /**
     * CartItem DO 转 Domain
     */
    public CartItem toItemDomain(CartItemDO itemDO) {
        if (itemDO == null) {
            return null;
        }
        Money unitPrice = new Money(itemDO.getUnitPrice(), itemDO.getCurrency());
        Money subtotal = new Money(itemDO.getSubtotal(), itemDO.getCurrency());

        return CartItem.reconstruct(
                itemDO.getProductId(),
                itemDO.getProductName(),
                unitPrice,
                new Quantity(itemDO.getQuantity()),
                subtotal
        );
    }

    /**
     * CartItem Domain 转 DO
     */
    public CartItemDO toItemDO(CartItem item, Long cartId) {
        if (item == null) {
            return null;
        }
        CartItemDO itemDO = new CartItemDO();
        itemDO.setCartId(cartId);
        itemDO.setProductId(item.getProductId());
        itemDO.setProductName(item.getProductName());
        itemDO.setQuantity(item.getQuantity().getValue());
        itemDO.setUnitPrice(item.getUnitPrice().getAmount());
        itemDO.setCurrency(item.getUnitPrice().getCurrency());
        itemDO.setSubtotal(item.getSubtotal().getAmount());
        itemDO.setDeleted(0);
        return itemDO;
    }

    /**
     * CartItem Domain 列表转 DO 列表
     */
    public List<CartItemDO> toItemDOList(List<CartItem> items, Long cartId) {
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
                .map(item -> toItemDO(item, cartId))
                .collect(Collectors.toList());
    }
}
