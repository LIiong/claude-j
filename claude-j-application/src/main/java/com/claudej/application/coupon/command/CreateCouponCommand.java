package com.claudej.application.coupon.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建优惠券命令
 */
@Data
public class CreateCouponCommand {

    private String name;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private String userId;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}
