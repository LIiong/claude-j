package com.claudej.application.coupon.assembler;

import com.claudej.application.coupon.dto.CouponDTO;
import com.claudej.domain.coupon.model.aggregate.Coupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Coupon 转换器
 */
@Mapper(componentModel = "spring")
public interface CouponAssembler {

    /**
     * Domain 转 DTO
     */
    @Mapping(target = "couponId", expression = "java(coupon.getCouponIdValue())")
    @Mapping(target = "discountType", expression = "java(coupon.getDiscountType().name())")
    @Mapping(target = "discountValue", expression = "java(coupon.getDiscountValue().getValue())")
    @Mapping(target = "status", expression = "java(coupon.getStatus().name())")
    CouponDTO toDTO(Coupon coupon);

    /**
     * Domain 列表转 DTO 列表
     */
    List<CouponDTO> toDTOList(List<Coupon> coupons);
}
