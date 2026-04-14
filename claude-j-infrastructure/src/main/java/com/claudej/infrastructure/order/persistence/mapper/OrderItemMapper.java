package com.claudej.infrastructure.order.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.order.persistence.dataobject.OrderItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项 Mapper
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {
}
