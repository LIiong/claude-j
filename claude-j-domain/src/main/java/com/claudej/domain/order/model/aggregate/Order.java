package com.claudej.domain.order.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.model.valobj.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 订单聚合根 - 封装订单业务不变量
 */
@Getter
public class Order {

    private Long id;
    private OrderId orderId;
    private CustomerId customerId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private Money totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Order(CustomerId customerId, LocalDateTime createTime) {
        this.customerId = customerId;
        this.status = OrderStatus.CREATED;
        this.items = new ArrayList<>();
        this.totalAmount = Money.cny(0);
        this.createTime = createTime;
        this.updateTime = createTime;
    }

    /**
     * 工厂方法：创建新订单
     */
    public static Order create(CustomerId customerId) {
        if (customerId == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "客户ID不能为空");
        }
        Order order = new Order(customerId, LocalDateTime.now());
        order.orderId = new OrderId(UUID.randomUUID().toString().replace("-", ""));
        return order;
    }

    /**
     * 从持久化层重建聚合根
     */
    public static Order reconstruct(Long id, OrderId orderId, CustomerId customerId,
                                     OrderStatus status, List<OrderItem> items,
                                     Money totalAmount, LocalDateTime createTime, LocalDateTime updateTime) {
        Order order = new Order(customerId, createTime);
        order.id = id;
        order.orderId = orderId;
        order.status = status;
        if (items != null) {
            order.items.addAll(items);
        }
        order.totalAmount = totalAmount;
        order.updateTime = updateTime;
        return order;
    }

    /**
     * 添加订单项
     */
    public void addItem(OrderItem item) {
        if (item == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单项不能为空");
        }
        items.add(item);
        recalculateTotal();
        updateTime = LocalDateTime.now();
    }

    /**
     * 支付订单
     */
    public void pay() {
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单项不能为空，无法支付");
        }
        this.status = this.status.toPaid();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 发货
     */
    public void ship() {
        this.status = this.status.toShipped();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 确认送达
     */
    public void deliver() {
        this.status = this.status.toDelivered();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 取消订单
     */
    public void cancel() {
        this.status = this.status.toCancelled();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 重新计算订单总金额
     */
    private void recalculateTotal() {
        Money total = Money.cny(0);
        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }
        this.totalAmount = total;
    }

    /**
     * 设置数据库自增 ID（持久化后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取订单项列表（不可修改）
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * 便捷获取订单号字符串值
     */
    public String getOrderIdValue() {
        return orderId.getValue();
    }

    /**
     * 便捷获取客户ID字符串值
     */
    public String getCustomerIdValue() {
        return customerId.getValue();
    }

    /**
     * 是否已支付
     */
    public boolean isPaid() {
        return status == OrderStatus.PAID || status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED;
    }

    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    /**
     * 是否已完成
     */
    public boolean isDelivered() {
        return status == OrderStatus.DELIVERED;
    }
}
