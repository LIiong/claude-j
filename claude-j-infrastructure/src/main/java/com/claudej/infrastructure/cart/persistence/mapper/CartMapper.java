package com.claudej.infrastructure.cart.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.cart.persistence.dataobject.CartDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车 Mapper
 */
@Mapper
public interface CartMapper extends BaseMapper<CartDO> {
}
