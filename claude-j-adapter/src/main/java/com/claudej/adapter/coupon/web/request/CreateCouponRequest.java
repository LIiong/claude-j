package com.claudej.adapter.coupon.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建优惠券请求
 */
@Data
public class CreateCouponRequest {

    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @NotBlank(message = "折扣类型不能为空")
    private String discountType;

    @NotNull(message = "折扣值不能为空")
    @Positive(message = "折扣值必须大于0")
    private BigDecimal discountValue;

    @NotNull(message = "最低订单金额不能为空")
    @Positive(message = "最低订单金额必须大于等于0")
    private BigDecimal minOrderAmount;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotNull(message = "有效期开始时间不能为空")
    private LocalDateTime validFrom;

    @NotNull(message = "有效期截止时间不能为空")
    private LocalDateTime validUntil;
}
