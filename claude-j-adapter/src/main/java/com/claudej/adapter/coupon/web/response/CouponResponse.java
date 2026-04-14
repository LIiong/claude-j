package com.claudej.adapter.coupon.web.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券响应
 */
@Data
public class CouponResponse {

    private String couponId;
    private String name;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private String currency;
    private String status;
    private String userId;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime usedTime;
    private String usedOrderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
