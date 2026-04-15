package com.claudej.application.cart.assembler;

import com.claudej.application.cart.dto.CartDTO;
import com.claudej.application.cart.dto.CartItemDTO;
import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.model.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车组装器
 */
@Mapper(componentModel = "spring")
public interface CartAssembler {

    CartAssembler INSTANCE = Mappers.getMapper(CartAssembler.class);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "items", target = "items", qualifiedByName = "toItemDTOList")
    @Mapping(source = "totalAmount.amount", target = "totalAmount")
    @Mapping(source = "totalAmount.currency", target = "currency")
    @Mapping(source = "items", target = "itemCount", qualifiedByName = "calculateItemCount")
    @Mapping(source = "updateTime", target = "updateTime")
    CartDTO toDTO(Cart cart);

    @Mapping(source = "productId", target = "productId")
    @Mapping(source = "productName", target = "productName")
    @Mapping(source = "unitPrice.amount", target = "unitPrice")
    @Mapping(source = "quantity.value", target = "quantity")
    @Mapping(source = "subtotal.amount", target = "subtotal")
    CartItemDTO toItemDTO(CartItem item);

    @Named("toItemDTOList")
    default List<CartItemDTO> toItemDTOList(List<CartItem> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
            .map(this::toItemDTO)
            .collect(Collectors.toList());
    }

    @Named("calculateItemCount")
    default Integer calculateItemCount(List<CartItem> items) {
        return items == null ? 0 : items.size();
    }
}
