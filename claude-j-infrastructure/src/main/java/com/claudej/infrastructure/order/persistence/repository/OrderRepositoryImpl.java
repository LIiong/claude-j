package com.claudej.infrastructure.order.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.repository.OrderRepository;
import com.claudej.infrastructure.common.persistence.PageHelper;
import com.claudej.infrastructure.order.persistence.converter.OrderConverter;
import com.claudej.infrastructure.order.persistence.dataobject.OrderDO;
import com.claudej.infrastructure.order.persistence.dataobject.OrderItemDO;
import com.claudej.infrastructure.order.persistence.mapper.OrderItemMapper;
import com.claudej.infrastructure.order.persistence.mapper.OrderMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 订单 Repository 实现
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderConverter orderConverter;

    public OrderRepositoryImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                               OrderConverter orderConverter) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderConverter = orderConverter;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        if (order.getId() == null) {
            // 新增订单
            OrderDO orderDO = orderConverter.toDO(order);
            orderDO.setCreateTime(LocalDateTime.now());
            orderDO.setUpdateTime(LocalDateTime.now());
            orderDO.setDeleted(0);
            orderMapper.insert(orderDO);
            order.setId(orderDO.getId());

            // 插入订单项
            List<OrderItemDO> itemDOList = orderConverter.toItemDOList(order.getItems(), order.getOrderIdValue());
            for (OrderItemDO itemDO : itemDOList) {
                itemDO.setCreateTime(LocalDateTime.now());
                itemDO.setUpdateTime(LocalDateTime.now());
                itemDO.setDeleted(0);
                orderItemMapper.insert(itemDO);
            }
        } else {
            // 更新订单
            OrderDO orderDO = orderConverter.toDO(order);
            orderDO.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(orderDO);

            // 删除旧订单项，插入新订单项
            LambdaQueryWrapper<OrderItemDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderItemDO::getOrderId, order.getOrderIdValue());
            orderItemMapper.delete(wrapper);

            List<OrderItemDO> itemDOList = orderConverter.toItemDOList(order.getItems(), order.getOrderIdValue());
            for (OrderItemDO itemDO : itemDOList) {
                itemDO.setCreateTime(LocalDateTime.now());
                itemDO.setUpdateTime(LocalDateTime.now());
                itemDO.setDeleted(0);
                orderItemMapper.insert(itemDO);
            }
        }
        return order;
    }

    @Override
    public Optional<Order> findByOrderId(OrderId orderId) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDO::getOrderId, orderId.getValue());
        OrderDO orderDO = orderMapper.selectOne(wrapper);

        if (orderDO == null) {
            return Optional.empty();
        }

        // 查询订单项
        LambdaQueryWrapper<OrderItemDO> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItemDO::getOrderId, orderId.getValue());
        List<OrderItemDO> itemDOList = orderItemMapper.selectList(itemWrapper);

        Order order = orderConverter.toDomain(orderDO, itemDOList);
        return Optional.ofNullable(order);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDO::getCustomerId, customerId.getValue());
        List<OrderDO> orderDOList = orderMapper.selectList(wrapper);

        return orderDOList.stream()
                .map(orderDO -> {
                    LambdaQueryWrapper<OrderItemDO> itemWrapper = new LambdaQueryWrapper<>();
                    itemWrapper.eq(OrderItemDO::getOrderId, orderDO.getOrderId());
                    List<OrderItemDO> itemDOList = orderItemMapper.selectList(itemWrapper);
                    return orderConverter.toDomain(orderDO, itemDOList);
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrderId(OrderId orderId) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDO::getOrderId, orderId.getValue());
        return orderMapper.selectCount(wrapper) > 0;
    }

    @Override
    public com.claudej.domain.common.model.valobj.Page<Order> findByCustomerId(CustomerId customerId, PageRequest pageRequest) {
        Page<OrderDO> mybatisPage = PageHelper.createMybatisPlusPage(pageRequest);
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDO::getCustomerId, customerId.getValue());
        IPage<OrderDO> iPage = orderMapper.selectPage(mybatisPage, wrapper);

        java.util.List<Order> orders = iPage.getRecords().stream()
                .map(orderDO -> {
                    LambdaQueryWrapper<OrderItemDO> itemWrapper = new LambdaQueryWrapper<>();
                    itemWrapper.eq(OrderItemDO::getOrderId, orderDO.getOrderId());
                    List<OrderItemDO> itemDOList = orderItemMapper.selectList(itemWrapper);
                    return orderConverter.toDomain(orderDO, itemDOList);
                })
                .collect(Collectors.toList());

        return new com.claudej.domain.common.model.valobj.Page<Order>(
                orders,
                iPage.getTotal(),
                (int) iPage.getCurrent() - 1,
                (int) iPage.getSize()
        );
    }
}
