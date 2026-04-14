package com.claudej.application.user.service;

import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.order.service.OrderApplicationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户订单查询服务
 * 通过 OrderApplicationService 查询用户订单（不直接访问 OrderRepository）
 */
@Service
public class UserOrderQueryService {

    private final OrderApplicationService orderApplicationService;

    public UserOrderQueryService(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    /**
     * 查询用户订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    public List<OrderDTO> getUserOrders(String userId) {
        // 通过 OrderApplicationService 查询订单
        // Order.customerId 对应 User.userId
        return orderApplicationService.getOrdersByCustomerId(userId);
    }

    /**
     * 查询用户订单详情
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @return 订单详情
     */
    public OrderDTO getUserOrderDetail(String userId, String orderId) {
        // 查询订单
        OrderDTO order = orderApplicationService.getOrderById(orderId);

        // 验证订单归属
        if (order == null || !order.getCustomerId().equals(userId)) {
            return null;
        }

        return order;
    }
}
