package com.claudej.domain.coupon.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.model.valobj.CouponStatus;
import com.claudej.domain.coupon.model.valobj.DiscountType;
import com.claudej.domain.coupon.model.valobj.DiscountValue;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 优惠券聚合根 - 封装优惠券全生命周期的业务不变量
 */
@Getter
public class Coupon {

    private Long id;
    private CouponId couponId;
    private String name;
    private DiscountType discountType;
    private DiscountValue discountValue;
    private BigDecimal minOrderAmount;
    private String currency;
    private CouponStatus status;
    private String userId;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime usedTime;
    private String usedOrderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Coupon() {
    }

    /**
     * 工厂方法：创建新优惠券
     */
    public static Coupon create(String name, DiscountType discountType, BigDecimal discountValue,
                                BigDecimal minOrderAmount, String userId,
                                LocalDateTime validFrom, LocalDateTime validUntil) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.COUPON_NAME_EMPTY, "优惠券名称不能为空");
        }
        if (name.trim().length() > 50) {
            throw new BusinessException(ErrorCode.COUPON_NAME_TOO_LONG, "优惠券名称长度不能超过50");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.COUPON_USER_ID_EMPTY, "用户ID不能为空");
        }
        if (validFrom == null || validUntil == null) {
            throw new BusinessException(ErrorCode.COUPON_VALIDITY_INVALID, "有效期不能为空");
        }
        if (!validFrom.isBefore(validUntil)) {
            throw new BusinessException(ErrorCode.COUPON_VALIDITY_INVALID,
                    "有效期开始时间必须早于截止时间");
        }
        if (minOrderAmount == null) {
            minOrderAmount = BigDecimal.ZERO;
        }
        if (minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_INVALID,
                    "最低订单金额不能为负数");
        }

        Coupon coupon = new Coupon();
        coupon.couponId = new CouponId(UUID.randomUUID().toString().replace("-", ""));
        coupon.name = name.trim();
        coupon.discountType = discountType;
        coupon.discountValue = new DiscountValue(discountValue, discountType);
        coupon.minOrderAmount = minOrderAmount;
        coupon.currency = "CNY";
        coupon.status = CouponStatus.AVAILABLE;
        coupon.userId = userId.trim();
        coupon.validFrom = validFrom;
        coupon.validUntil = validUntil;
        coupon.createTime = LocalDateTime.now();
        coupon.updateTime = LocalDateTime.now();
        return coupon;
    }

    /**
     * 从持久化层重建聚合根（不执行业务校验）
     */
    public static Coupon reconstruct(Long id, CouponId couponId, String name,
                                      DiscountType discountType, DiscountValue discountValue,
                                      BigDecimal minOrderAmount, String currency,
                                      CouponStatus status, String userId,
                                      LocalDateTime validFrom, LocalDateTime validUntil,
                                      LocalDateTime usedTime, String usedOrderId,
                                      LocalDateTime createTime, LocalDateTime updateTime) {
        Coupon coupon = new Coupon();
        coupon.id = id;
        coupon.couponId = couponId;
        coupon.name = name;
        coupon.discountType = discountType;
        coupon.discountValue = discountValue;
        coupon.minOrderAmount = minOrderAmount;
        coupon.currency = currency;
        coupon.status = status;
        coupon.userId = userId;
        coupon.validFrom = validFrom;
        coupon.validUntil = validUntil;
        coupon.usedTime = usedTime;
        coupon.usedOrderId = usedOrderId;
        coupon.createTime = createTime;
        coupon.updateTime = updateTime;
        return coupon;
    }

    /**
     * 使用优惠券 - 校验状态和有效期，关联订单号
     */
    public void use(String orderId, LocalDateTime now) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.COUPON_ORDER_ID_EMPTY, "订单号不能为空");
        }
        // 先检查是否过期（懒过期）
        checkAndExpire(now);
        if (this.status != CouponStatus.AVAILABLE) {
            this.status = this.status.toUsed(); // 会抛异常
        }
        // 校验有效期
        if (now.isBefore(validFrom)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_YET_VALID, "优惠券尚未生效");
        }
        this.status = this.status.toUsed();
        this.usedTime = now;
        this.usedOrderId = orderId.trim();
        this.updateTime = now;
    }

    /**
     * 检查并标记过期（懒过期策略）
     * 注意：这是查询路径上的副作用，查询时如果过期会自动转换状态并需持久化
     */
    public boolean checkAndExpire(LocalDateTime now) {
        if (this.status == CouponStatus.AVAILABLE && now.isAfter(validUntil)) {
            this.status = this.status.toExpired();
            this.updateTime = now;
            return true;
        }
        return false;
    }

    /**
     * 计算折扣金额
     * FIXED_AMOUNT: 返回固定折扣值（不超过订单金额）
     * PERCENTAGE: 返回 orderAmount * percentage / 100（向下取整到分）
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (discountType == DiscountType.FIXED_AMOUNT) {
            // 不超过订单金额
            BigDecimal discount = discountValue.getValue();
            return discount.compareTo(orderAmount) > 0 ? orderAmount : discount;
        } else {
            // 百分比折扣，向下取整到分
            return orderAmount.multiply(discountValue.getValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.DOWN);
        }
    }

    /**
     * 设置数据库自增 ID（持久化后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 便捷获取优惠券ID字符串值
     */
    public String getCouponIdValue() {
        return couponId.getValue();
    }
}
