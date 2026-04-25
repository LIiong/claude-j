package com.claudej.infrastructure.product.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品数据对象
 */
@Data
@TableName("t_product")
public class ProductDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productId;

    private String name;

    private String description;

    private String skuCode;

    private Integer stock;

    private BigDecimal originalPrice;

    private BigDecimal promotionalPrice;

    private String currency;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}