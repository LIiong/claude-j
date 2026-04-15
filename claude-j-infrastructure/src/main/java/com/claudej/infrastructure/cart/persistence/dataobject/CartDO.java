package com.claudej.infrastructure.cart.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车数据对象
 */
@Data
@TableName("t_cart")
public class CartDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private BigDecimal totalAmount;

    private String currency;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
