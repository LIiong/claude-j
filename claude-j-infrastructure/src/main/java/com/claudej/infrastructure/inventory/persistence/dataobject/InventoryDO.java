package com.claudej.infrastructure.inventory.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存数据对象
 */
@Data
@TableName("t_inventory")
public class InventoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String inventoryId;

    private String productId;

    private String skuCode;

    private Integer availableStock;

    private Integer reservedStock;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}