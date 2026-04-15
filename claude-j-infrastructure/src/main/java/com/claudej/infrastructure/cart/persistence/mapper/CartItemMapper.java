package com.claudej.infrastructure.cart.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.cart.persistence.dataobject.CartItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 购物车项 Mapper
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItemDO> {

    /**
     * 根据购物车ID查询购物车项列表
     */
    @Select("SELECT * FROM t_cart_item WHERE cart_id = #{cartId} AND deleted = 0")
    List<CartItemDO> selectByCartId(@Param("cartId") Long cartId);
}
