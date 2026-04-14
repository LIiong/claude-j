package com.claudej.application.order.service;

import com.claudej.application.order.assembler.OrderAssembler;
import com.claudej.application.order.command.CreateOrderCommand;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单应用服务
 */
@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public OrderApplicationService(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }

    /**
     * 创建订单
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderCommand command) {
        if (command == null || command.getCustomerId() == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "客户ID不能为空");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单项不能为空");
        }

        CustomerId customerId = new CustomerId(command.getCustomerId());
        Order order = Order.create(customerId);

        for (CreateOrderCommand.OrderItemCommand itemCmd : command.getItems()) {
            OrderItem item = OrderItem.create(
                    itemCmd.getProductId(),
                    itemCmd.getProductName(),
                    itemCmd.getQuantity(),
                    new Money(itemCmd.getUnitPrice(), "CNY")
            );
            order.addItem(item);
        }

        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    /**
     * 根据订单号查询订单
     */
    public OrderDTO getOrderById(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return orderAssembler.toDTO(order);
    }

    /**
     * 根据客户ID查询订单列表
     */
    public List<OrderDTO> getOrdersByCustomerId(String customerId) {
        List<Order> orders = orderRepository.findByCustomerId(new CustomerId(customerId));
        return orderAssembler.toDTOList(orders);
    }

    /**
     * 支付订单
     */
    @Transactional
    public OrderDTO payOrder(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.pay();
        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    /**
     * 取消订单
     */
    @Transactional
    public OrderDTO cancelOrder(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();
        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }
}
