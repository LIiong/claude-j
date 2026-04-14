package com.claudej.infrastructure.coupon.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券数据对象
 */
@Data
@TableName("t_coupon")
public class CouponDO {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    @TableLogic
    private Integer deleted;
}
