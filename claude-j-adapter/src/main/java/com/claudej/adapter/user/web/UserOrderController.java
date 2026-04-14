package com.claudej.adapter.user.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.order.web.response.OrderResponse;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.user.service.UserOrderQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户订单 Controller
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/orders")
public class UserOrderController {

    private final UserOrderQueryService userOrderQueryService;

    public UserOrderController(UserOrderQueryService userOrderQueryService) {
        this.userOrderQueryService = userOrderQueryService;
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping
    public ApiResult<List<OrderResponse>> getUserOrders(@PathVariable String userId) {
        List<OrderDTO> dtoList = userOrderQueryService.getUserOrders(userId);
        List<OrderResponse> responseList = dtoList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResult.ok(responseList);
    }

    /**
     * 查询用户订单详情
     */
    @GetMapping("/{orderId}")
    public ApiResult<OrderResponse> getUserOrderDetail(@PathVariable String userId, @PathVariable String orderId) {
        OrderDTO dto = userOrderQueryService.getUserOrderDetail(userId, orderId);
        if (dto == null) {
            return ApiResult.fail("ORDER_NOT_FOUND", "订单不存在或无权限查看");
        }
        OrderResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    private OrderResponse convertToResponse(OrderDTO dto) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(dto.getOrderId());
        response.setCustomerId(dto.getCustomerId());
        response.setStatus(dto.getStatus());
        response.setTotalAmount(dto.getTotalAmount());
        response.setCurrency(dto.getCurrency());
        response.setCreateTime(dto.getCreateTime());
        response.setUpdateTime(dto.getUpdateTime());
        return response;
    }
}
