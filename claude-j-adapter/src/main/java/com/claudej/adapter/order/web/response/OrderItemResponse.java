package com.claudej.adapter.order.web.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项响应
 */
@Data
public class OrderItemResponse {

    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
