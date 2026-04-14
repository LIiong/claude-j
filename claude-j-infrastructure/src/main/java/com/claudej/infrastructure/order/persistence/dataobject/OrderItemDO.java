package com.claudej.infrastructure.order.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项数据对象
 */
@Data
@TableName("t_order_item")
public class OrderItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderId;

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
