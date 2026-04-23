package com.claudej.application.coupon.service;

import com.claudej.application.common.assembler.PageAssembler;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.coupon.assembler.CouponAssembler;
import com.claudej.application.coupon.command.CreateCouponCommand;
import com.claudej.application.coupon.command.UseCouponCommand;
import com.claudej.application.coupon.dto.CouponDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.model.valobj.DiscountType;
import com.claudej.domain.coupon.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券应用服务
 */
@Service
public class CouponApplicationService {

    private final CouponRepository couponRepository;
    private final CouponAssembler couponAssembler;
    private final PageAssembler pageAssembler;

    public CouponApplicationService(CouponRepository couponRepository, CouponAssembler couponAssembler, PageAssembler pageAssembler) {
        this.couponRepository = couponRepository;
        this.couponAssembler = couponAssembler;
        this.pageAssembler = pageAssembler;
    }

    /**
     * 创建优惠券
     */
    @Transactional
    public CouponDTO createCoupon(CreateCouponCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.COUPON_NAME_EMPTY, "命令不能为空");
        }

        DiscountType discountType = DiscountType.valueOf(command.getDiscountType());

        Coupon coupon = Coupon.create(
                command.getName(),
                discountType,
                command.getDiscountValue(),
                command.getMinOrderAmount(),
                command.getUserId(),
                command.getValidFrom(),
                command.getValidUntil()
        );

        coupon = couponRepository.save(coupon);
        return couponAssembler.toDTO(coupon);
    }

    /**
     * 根据优惠券ID查询
     */
    public CouponDTO getCouponById(String couponId) {
        Coupon coupon = couponRepository.findByCouponId(new CouponId(couponId))
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        return couponAssembler.toDTO(coupon);
    }

    /**
     * 根据用户ID查询所有优惠券
     */
    public List<CouponDTO> getCouponsByUserId(String userId) {
        List<Coupon> coupons = couponRepository.findByUserId(userId);
        return couponAssembler.toDTOList(coupons);
    }

    /**
     * 根据用户ID查询可用优惠券
     */
    public List<CouponDTO> getAvailableCouponsByUserId(String userId) {
        List<Coupon> coupons = couponRepository.findAvailableByUserId(userId);
        return couponAssembler.toDTOList(coupons);
    }

    /**
     * 分页查询用户所有优惠券
     */
    public PageDTO<CouponDTO> getCouponsByUserId(String userId, PageRequest pageRequest) {
        com.claudej.domain.common.model.valobj.Page<Coupon> page = couponRepository.findByUserId(userId, pageRequest);
        return pageAssembler.toPageDTO(page, couponAssembler::toDTO);
    }

    /**
     * 分页查询用户可用优惠券
     */
    public PageDTO<CouponDTO> getAvailableCouponsByUserId(String userId, PageRequest pageRequest) {
        com.claudej.domain.common.model.valobj.Page<Coupon> page = couponRepository.findAvailableByUserId(userId, pageRequest);
        return pageAssembler.toPageDTO(page, couponAssembler::toDTO);
    }

    /**
     * 使用优惠券
     */
    @Transactional
    public CouponDTO useCoupon(String couponId, UseCouponCommand command) {
        if (command == null || command.getOrderId() == null) {
            throw new BusinessException(ErrorCode.COUPON_ORDER_ID_EMPTY, "订单号不能为空");
        }

        Coupon coupon = couponRepository.findByCouponId(new CouponId(couponId))
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        coupon.use(command.getOrderId(), LocalDateTime.now());
        coupon = couponRepository.save(coupon);
        return couponAssembler.toDTO(coupon);
    }
}
