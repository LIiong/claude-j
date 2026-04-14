package com.claudej.infrastructure.coupon.persistence.converter;

import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.model.valobj.CouponStatus;
import com.claudej.domain.coupon.model.valobj.DiscountType;
import com.claudej.domain.coupon.model.valobj.DiscountValue;
import com.claudej.infrastructure.coupon.persistence.dataobject.CouponDO;
import org.springframework.stereotype.Component;

/**
 * Coupon 转换器
 */
@Component
public class CouponConverter {

    /**
     * DO 转 Domain
     */
    public Coupon toDomain(CouponDO couponDO) {
        if (couponDO == null) {
            return null;
        }

        return Coupon.reconstruct(
                couponDO.getId(),
                new CouponId(couponDO.getCouponId()),
                couponDO.getName(),
                DiscountType.valueOf(couponDO.getDiscountType()),
                new DiscountValue(couponDO.getDiscountValue(), DiscountType.valueOf(couponDO.getDiscountType())),
                couponDO.getMinOrderAmount(),
                couponDO.getCurrency(),
                CouponStatus.valueOf(couponDO.getStatus()),
                couponDO.getUserId(),
                couponDO.getValidFrom(),
                couponDO.getValidUntil(),
                couponDO.getUsedTime(),
                couponDO.getUsedOrderId(),
                couponDO.getCreateTime(),
                couponDO.getUpdateTime()
        );
    }

    /**
     * Domain 转 DO
     */
    public CouponDO toDO(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        CouponDO couponDO = new CouponDO();
        couponDO.setId(coupon.getId());
        couponDO.setCouponId(coupon.getCouponIdValue());
        couponDO.setName(coupon.getName());
        couponDO.setDiscountType(coupon.getDiscountType().name());
        couponDO.setDiscountValue(coupon.getDiscountValue().getValue());
        couponDO.setMinOrderAmount(coupon.getMinOrderAmount());
        couponDO.setCurrency(coupon.getCurrency());
        couponDO.setStatus(coupon.getStatus().name());
        couponDO.setUserId(coupon.getUserId());
        couponDO.setValidFrom(coupon.getValidFrom());
        couponDO.setValidUntil(coupon.getValidUntil());
        couponDO.setUsedTime(coupon.getUsedTime());
        couponDO.setUsedOrderId(coupon.getUsedOrderId());
        couponDO.setCreateTime(coupon.getCreateTime());
        couponDO.setUpdateTime(coupon.getUpdateTime());
        couponDO.setDeleted(0);
        return couponDO;
    }
}
