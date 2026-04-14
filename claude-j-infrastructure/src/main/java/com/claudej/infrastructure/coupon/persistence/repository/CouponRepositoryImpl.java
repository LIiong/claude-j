package com.claudej.infrastructure.coupon.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.model.valobj.CouponStatus;
import com.claudej.domain.coupon.repository.CouponRepository;
import com.claudej.infrastructure.coupon.persistence.converter.CouponConverter;
import com.claudej.infrastructure.coupon.persistence.dataobject.CouponDO;
import com.claudej.infrastructure.coupon.persistence.mapper.CouponMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 优惠券 Repository 实现
 */
@Repository
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponMapper couponMapper;
    private final CouponConverter couponConverter;

    public CouponRepositoryImpl(CouponMapper couponMapper, CouponConverter couponConverter) {
        this.couponMapper = couponMapper;
        this.couponConverter = couponConverter;
    }

    @Override
    @Transactional
    public Coupon save(Coupon coupon) {
        CouponDO couponDO = couponConverter.toDO(coupon);

        // 处理懒过期：查询时标记为 AVAILABLE 但实际已过期的优惠券
        if (coupon.getStatus() == CouponStatus.AVAILABLE
                && couponDO.getValidUntil() != null
                && LocalDateTime.now().isAfter(couponDO.getValidUntil())) {
            // 已在聚合中处理过期，转换器会反映最新状态
            couponDO.setStatus(CouponStatus.EXPIRED.name());
        }

        if (coupon.getId() == null) {
            // 新增
            couponDO.setCreateTime(LocalDateTime.now());
            couponDO.setUpdateTime(LocalDateTime.now());
            couponDO.setDeleted(0);
            couponMapper.insert(couponDO);
            coupon.setId(couponDO.getId());
        } else {
            // 更新
            couponDO.setUpdateTime(LocalDateTime.now());
            couponMapper.updateById(couponDO);
        }

        return coupon;
    }

    @Override
    public Optional<Coupon> findByCouponId(CouponId couponId) {
        LambdaQueryWrapper<CouponDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponDO::getCouponId, couponId.getValue());
        CouponDO couponDO = couponMapper.selectOne(wrapper);

        if (couponDO == null) {
            return Optional.empty();
        }

        Coupon coupon = couponConverter.toDomain(couponDO);

        // 懒过期处理：如果当前时间超过有效期且状态为 AVAILABLE，自动转为 EXPIRED
        if (coupon != null && coupon.getStatus() == CouponStatus.AVAILABLE) {
            if (coupon.checkAndExpire(LocalDateTime.now())) {
                // 状态已变更，需要持久化
                CouponDO updatedDO = couponConverter.toDO(coupon);
                updatedDO.setUpdateTime(LocalDateTime.now());
                couponMapper.updateById(updatedDO);
            }
        }

        return Optional.ofNullable(coupon);
    }

    @Override
    public List<Coupon> findByUserId(String userId) {
        LambdaQueryWrapper<CouponDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponDO::getUserId, userId);
        List<CouponDO> couponDOList = couponMapper.selectList(wrapper);

        return couponDOList.stream()
                .map(couponConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Coupon> findAvailableByUserId(String userId) {
        LambdaQueryWrapper<CouponDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponDO::getUserId, userId);

        // 查询所有非已使用的优惠券，过期状态在内存中判断
        wrapper.ne(CouponDO::getStatus, CouponStatus.USED.name());

        List<CouponDO> couponDOList = couponMapper.selectList(wrapper);

        return couponDOList.stream()
                .map(couponConverter::toDomain)
                .filter(coupon -> {
                    if (coupon == null) {
                        return false;
                    }
                    // 懒过期处理
                    if (coupon.getStatus() == CouponStatus.AVAILABLE) {
                        if (coupon.checkAndExpire(LocalDateTime.now())) {
                            // 状态已变更，需要持久化
                            CouponDO updatedDO = couponConverter.toDO(coupon);
                            updatedDO.setUpdateTime(LocalDateTime.now());
                            couponMapper.updateById(updatedDO);
                        }
                    }
                    return coupon.getStatus() == CouponStatus.AVAILABLE;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCouponId(CouponId couponId) {
        LambdaQueryWrapper<CouponDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponDO::getCouponId, couponId.getValue());
        return couponMapper.selectCount(wrapper) > 0;
    }
}
