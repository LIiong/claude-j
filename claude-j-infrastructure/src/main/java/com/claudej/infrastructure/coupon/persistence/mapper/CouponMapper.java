package com.claudej.infrastructure.coupon.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.coupon.persistence.dataobject.CouponDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券 Mapper
 */
@Mapper
public interface CouponMapper extends BaseMapper<CouponDO> {
}
