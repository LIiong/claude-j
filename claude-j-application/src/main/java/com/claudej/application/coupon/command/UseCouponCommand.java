package com.claudej.application.coupon.command;

import lombok.Data;

/**
 * 使用优惠券命令
 */
@Data
public class UseCouponCommand {

    private String orderId;
}
