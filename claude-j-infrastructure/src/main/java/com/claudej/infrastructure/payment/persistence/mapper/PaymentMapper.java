package com.claudej.infrastructure.payment.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.payment.persistence.dataobject.PaymentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付 Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<PaymentDO> {
}