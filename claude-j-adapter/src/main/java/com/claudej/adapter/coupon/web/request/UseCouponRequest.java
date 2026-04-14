package com.claudej.adapter.coupon.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 使用优惠券请求
 */
@Data
public class UseCouponRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderId;
}
