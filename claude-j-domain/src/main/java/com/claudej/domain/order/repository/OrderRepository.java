package com.claudej.domain.order.repository;

import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * 订单 Repository 端口接口
 */
public interface OrderRepository {

    /**
     * 保存订单
     */
    Order save(Order order);

    /**
     * 根据订单号查找订单
     */
    Optional<Order> findByOrderId(OrderId orderId);

    /**
     * 根据客户ID查询订单列表
     */
    List<Order> findByCustomerId(CustomerId customerId);

    /**
     * 检查订单是否存在
     */
    boolean existsByOrderId(OrderId orderId);
}
