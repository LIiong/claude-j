package com.claudej.adapter.cart.web;

import com.claudej.adapter.cart.web.request.AddCartItemRequest;
import com.claudej.adapter.cart.web.request.UpdateCartItemQuantityRequest;
import com.claudej.adapter.cart.web.response.CartItemResponse;
import com.claudej.adapter.cart.web.response.CartResponse;
import com.claudej.adapter.common.ApiResult;
import com.claudej.application.cart.command.AddCartItemCommand;
import com.claudej.application.cart.command.UpdateCartItemQuantityCommand;
import com.claudej.application.cart.dto.CartDTO;
import com.claudej.application.cart.service.CartApplicationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车 Controller
 */
@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    private final CartApplicationService cartApplicationService;

    public CartController(CartApplicationService cartApplicationService) {
        this.cartApplicationService = cartApplicationService;
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/items")
    public ApiResult<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        AddCartItemCommand command = new AddCartItemCommand();
        command.setUserId(request.getUserId());
        command.setProductId(request.getProductId());
        command.setProductName(request.getProductName());
        command.setUnitPrice(request.getUnitPrice());
        command.setQuantity(request.getQuantity());

        CartDTO dto = cartApplicationService.addItem(command);
        CartResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 更新商品数量
     */
    @PutMapping("/items/quantity")
    public ApiResult<CartResponse> updateItemQuantity(@Valid @RequestBody UpdateCartItemQuantityRequest request) {
        UpdateCartItemQuantityCommand command = new UpdateCartItemQuantityCommand();
        command.setUserId(request.getUserId());
        command.setProductId(request.getProductId());
        command.setQuantity(request.getQuantity());

        CartDTO dto = cartApplicationService.updateItemQuantity(command);
        CartResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 删除购物车商品
     */
    @DeleteMapping("/items/{productId}")
    public ApiResult<CartResponse> removeItem(
            @RequestParam String userId,
            @PathVariable String productId) {
        CartDTO dto = cartApplicationService.removeItem(userId, productId);
        CartResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 清空购物车
     */
    @DeleteMapping
    public ApiResult<Void> clearCart(@RequestParam String userId) {
        cartApplicationService.clearCart(userId);
        return ApiResult.ok(null);
    }

    /**
     * 查询购物车
     */
    @GetMapping
    public ApiResult<CartResponse> getCart(@RequestParam String userId) {
        CartDTO dto = cartApplicationService.getCart(userId);
        CartResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    private CartResponse convertToResponse(CartDTO dto) {
        CartResponse response = new CartResponse();
        response.setUserId(dto.getUserId());
        response.setTotalAmount(dto.getTotalAmount());
        response.setCurrency(dto.getCurrency());
        response.setItemCount(dto.getItemCount());
        response.setUpdateTime(dto.getUpdateTime());

        if (dto.getItems() != null) {
            List<CartItemResponse> itemResponses = dto.getItems().stream()
                    .map(this::convertToItemResponse)
                    .collect(Collectors.toList());
            response.setItems(itemResponses);
        }

        return response;
    }

    private CartItemResponse convertToItemResponse(com.claudej.application.cart.dto.CartItemDTO dto) {
        CartItemResponse response = new CartItemResponse();
        response.setProductId(dto.getProductId());
        response.setProductName(dto.getProductName());
        response.setUnitPrice(dto.getUnitPrice());
        response.setQuantity(dto.getQuantity());
        response.setSubtotal(dto.getSubtotal());
        return response;
    }
}
