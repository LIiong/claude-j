package com.claudej.infrastructure.cart.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项数据对象
 */
@Data
@TableName("t_cart_item")
public class CartItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cartId;

    private String productId;

    private String productName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private String currency;

    private BigDecimal subtotal;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
