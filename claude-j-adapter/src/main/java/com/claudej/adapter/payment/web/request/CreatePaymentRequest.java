package com.claudej.adapter.payment.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 创建支付请求
 */
@Data
public class CreatePaymentRequest {

    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotBlank(message = "客户ID不能为空")
    private String customerId;

    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "支付方式不能为空")
    private String method;
}