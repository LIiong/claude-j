package com.claudej.adapter.order.web.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应
 */
@Data
public class OrderResponse {

    private String orderId;
    private String customerId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemResponse> items;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
