package com.claudej.application.order.assembler;

import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.order.dto.OrderItemDTO;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order 转换器
 */
@Mapper(componentModel = "spring")
public interface OrderAssembler {

    /**
     * Domain 转 DTO
     */
    @Mapping(target = "orderId", expression = "java(order.getOrderIdValue())")
    @Mapping(target = "customerId", expression = "java(order.getCustomerIdValue())")
    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "currency", expression = "java(order.getTotalAmount().getCurrency())")
    OrderDTO toDTO(Order order);

    /**
     * Domain 列表转 DTO 列表
     */
    List<OrderDTO> toDTOList(List<Order> orders);

    /**
     * OrderItem Domain 转 DTO
     */
    default OrderItemDTO toItemDTO(OrderItem item) {
        if (item == null) {
            return null;
        }
        OrderItemDTO dto = new OrderItemDTO();
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice().getAmount());
        dto.setSubtotal(item.getSubtotal().getAmount());
        return dto;
    }

    /**
     * OrderItem 列表转 DTO 列表
     */
    default List<OrderItemDTO> toItemDTOList(List<OrderItem> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    /**
     * 金额提取
     */
    default java.math.BigDecimal extractAmount(Money money) {
        return money == null ? null : money.getAmount();
    }
}
