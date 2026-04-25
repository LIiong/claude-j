package com.claudej.infrastructure.inventory.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.inventory.persistence.dataobject.InventoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存 Mapper
 */
@Mapper
public interface InventoryMapper extends BaseMapper<InventoryDO> {
}