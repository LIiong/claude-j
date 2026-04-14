package com.claudej.domain.coupon.repository;

import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;

import java.util.List;
import java.util.Optional;

/**
 * 优惠券 Repository 端口接口
 */
public interface CouponRepository {

    /**
     * 保存优惠券
     */
    Coupon save(Coupon coupon);

    /**
     * 根据优惠券业务ID查询
     */
    Optional<Coupon> findByCouponId(CouponId couponId);

    /**
     * 根据用户ID查询所有优惠券
     */
    List<Coupon> findByUserId(String userId);

    /**
     * 根据用户ID查询可用优惠券
     */
    List<Coupon> findAvailableByUserId(String userId);

    /**
     * 判断优惠券是否存在
     */
    boolean existsByCouponId(CouponId couponId);
}
