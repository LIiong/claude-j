package com.claudej.infrastructure.order.persistence.converter;

import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.model.valobj.OrderStatus;
import com.claudej.infrastructure.order.persistence.dataobject.OrderDO;
import com.claudej.infrastructure.order.persistence.dataobject.OrderItemDO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order 转换器
 */
@Component
public class OrderConverter {

    /**
     * Order DO 转 Domain
     */
    public Order toDomain(OrderDO orderDO, List<OrderItemDO> itemDOList) {
        if (orderDO == null) {
            return null;
        }

        List<OrderItem> items = itemDOList == null ? new ArrayList<>()
                : itemDOList.stream()
                        .map(this::toItemDomain)
                        .collect(Collectors.toList());

        Money totalAmount = new Money(orderDO.getTotalAmount(), orderDO.getCurrency());
        Money discountAmount = orderDO.getDiscountAmount() != null
                ? new Money(orderDO.getDiscountAmount(), orderDO.getCurrency())
                : Money.cny(0);
        Money finalAmount = orderDO.getFinalAmount() != null
                ? new Money(orderDO.getFinalAmount(), orderDO.getCurrency())
                : totalAmount;
        CouponId couponId = orderDO.getCouponId() != null ? new CouponId(orderDO.getCouponId()) : null;

        return Order.reconstruct(
                orderDO.getId(),
                new OrderId(orderDO.getOrderId()),
                new CustomerId(orderDO.getCustomerId()),
                OrderStatus.valueOf(orderDO.getStatus()),
                items,
                totalAmount,
                discountAmount,
                finalAmount,
                couponId,
                orderDO.getCreateTime(),
                orderDO.getUpdateTime()
        );
    }

    /**
     * Order Domain 转 DO
     */
    public OrderDO toDO(Order order) {
        if (order == null) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        orderDO.setId(order.getId());
        orderDO.setOrderId(order.getOrderIdValue());
        orderDO.setCustomerId(order.getCustomerIdValue());
        orderDO.setStatus(order.getStatus().name());
        orderDO.setTotalAmount(order.getTotalAmount().getAmount());
        if (order.getDiscountAmount() != null) {
            orderDO.setDiscountAmount(order.getDiscountAmount().getAmount());
        }
        if (order.getFinalAmount() != null) {
            orderDO.setFinalAmount(order.getFinalAmount().getAmount());
        }
        if (order.getCouponId() != null) {
            orderDO.setCouponId(order.getCouponIdValue());
        }
        orderDO.setCurrency(order.getTotalAmount().getCurrency());
        orderDO.setCreateTime(order.getCreateTime());
        orderDO.setUpdateTime(order.getUpdateTime());
        orderDO.setDeleted(0);
        return orderDO;
    }

    /**
     * OrderItem DO 转 Domain
     */
    public OrderItem toItemDomain(OrderItemDO itemDO) {
        if (itemDO == null) {
            return null;
        }
        Money unitPrice = new Money(itemDO.getUnitPrice(), itemDO.getCurrency());
        Money subtotal = new Money(itemDO.getSubtotal(), itemDO.getCurrency());

        return OrderItem.reconstruct(
                itemDO.getProductId(),
                itemDO.getProductName(),
                itemDO.getQuantity(),
                unitPrice,
                subtotal
        );
    }

    /**
     * OrderItem Domain 转 DO
     */
    public OrderItemDO toItemDO(OrderItem item, String orderId) {
        if (item == null) {
            return null;
        }
        OrderItemDO itemDO = new OrderItemDO();
        itemDO.setOrderId(orderId);
        itemDO.setProductId(item.getProductId());
        itemDO.setProductName(item.getProductName());
        itemDO.setQuantity(item.getQuantity());
        itemDO.setUnitPrice(item.getUnitPrice().getAmount());
        itemDO.setCurrency(item.getUnitPrice().getCurrency());
        itemDO.setSubtotal(item.getSubtotal().getAmount());
        itemDO.setDeleted(0);
        return itemDO;
    }

    /**
     * OrderItem Domain 列表转 DO 列表
     */
    public List<OrderItemDO> toItemDOList(List<OrderItem> items, String orderId) {
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
                .map(item -> toItemDO(item, orderId))
                .collect(Collectors.toList());
    }
}
