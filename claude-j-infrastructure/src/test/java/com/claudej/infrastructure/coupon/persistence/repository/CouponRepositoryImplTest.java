package com.claudej.infrastructure.coupon.persistence.repository;

import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.model.valobj.CouponStatus;
import com.claudej.domain.coupon.model.valobj.DiscountType;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponRepositoryImplTest {

    @SpringBootApplication(scanBasePackages = {"com.claudej.infrastructure", "com.claudej.application"})
    @MapperScan("com.claudej.infrastructure.**.mapper")
    static class TestConfig {
    }

    @Autowired
    private CouponRepositoryImpl couponRepository;

    @Test
    void should_saveNewCoupon_when_couponHasNoId() {
        // Given
        Coupon coupon = Coupon.create(
                "满100减20",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "USER001",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        // When
        Coupon savedCoupon = couponRepository.save(coupon);

        // Then
        assertThat(savedCoupon.getId()).isNotNull();
        assertThat(savedCoupon.getName()).isEqualTo("满100减20");
        assertThat(savedCoupon.getUserId()).isEqualTo("USER001");
    }

    @Test
    void should_findCouponByCouponId_when_couponExists() {
        // Given
        Coupon coupon = Coupon.create(
                "满200减50",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("50.00"),
                new BigDecimal("200.00"),
                "USER002",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        Coupon saved = couponRepository.save(coupon);

        // When
        Optional<Coupon> found = couponRepository.findByCouponId(new CouponId(saved.getCouponIdValue()));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("满200减50");
        assertThat(found.get().getUserId()).isEqualTo("USER002");
    }

    @Test
    void should_returnEmpty_when_couponNotFound() {
        // When
        Optional<Coupon> found = couponRepository.findByCouponId(new CouponId("NONEXISTENT"));

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void should_findCouponsByUserId_when_couponsExist() {
        // Given
        Coupon coupon1 = Coupon.create(
                "满100减20",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "USER003",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        Coupon coupon2 = Coupon.create(
                "8折优惠",
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("0"),
                "USER003",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        couponRepository.save(coupon1);
        couponRepository.save(coupon2);

        // When
        List<Coupon> coupons = couponRepository.findByUserId("USER003");

        // Then
        assertThat(coupons).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void should_findAvailableCoupons_when_couponsAvailable() {
        // Given
        Coupon coupon = Coupon.create(
                "满100减20",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "USER004",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        couponRepository.save(coupon);

        // When
        List<Coupon> coupons = couponRepository.findAvailableByUserId("USER004");

        // Then
        assertThat(coupons).isNotEmpty();
        assertThat(coupons.get(0).getStatus()).isEqualTo(CouponStatus.AVAILABLE);
    }

    @Test
    void should_returnTrue_when_couponExists() {
        // Given
        Coupon coupon = Coupon.create(
                "满100减20",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "USER005",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        Coupon saved = couponRepository.save(coupon);

        // When & Then
        assertThat(couponRepository.existsByCouponId(new CouponId(saved.getCouponIdValue()))).isTrue();
    }

    @Test
    void should_returnFalse_when_couponDoesNotExist() {
        // When & Then
        assertThat(couponRepository.existsByCouponId(new CouponId("NONEXISTENT"))).isFalse();
    }

    @Test
    void should_updateCoupon_when_couponHasId() {
        // Given
        Coupon coupon = Coupon.create(
                "满100减20",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "USER006",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        Coupon saved = couponRepository.save(coupon);

        // When - 使用优惠券
        saved.use("ORDER001", LocalDateTime.now());
        Coupon updated = couponRepository.save(saved);

        // Then
        Optional<Coupon> found = couponRepository.findByCouponId(new CouponId(updated.getCouponIdValue()));
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(CouponStatus.USED);
        assertThat(found.get().getUsedOrderId()).isEqualTo("ORDER001");
    }

    @Test
    void should_calculateDiscountFixedAmount_when_validOrderAmount() {
        // Given
        Coupon coupon = Coupon.create(
                "满100减20",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "USER007",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        // When
        BigDecimal discount = coupon.calculateDiscount(new BigDecimal("150.00"));

        // Then
        assertThat(discount).isEqualTo(new BigDecimal("20.00"));
    }

    @Test
    void should_calculateDiscountPercentage_when_validOrderAmount() {
        // Given
        Coupon coupon = Coupon.create(
                "8折优惠",
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("0"),
                "USER008",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        // When
        BigDecimal discount = coupon.calculateDiscount(new BigDecimal("100.00"));

        // Then
        assertThat(discount).isEqualTo(new BigDecimal("20.00"));
    }

    @Test
    void should_markExpired_when_findAvailableCouponsWithExpiredDate() {
        // Given - 创建一个已经过期的优惠券
        Coupon expiredCoupon = Coupon.create(
                "过期优惠券",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "USER009",
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1)  // 昨天过期
        );
        couponRepository.save(expiredCoupon);

        // When - 查询可用优惠券时，应自动标记为过期
        List<Coupon> availableCoupons = couponRepository.findAvailableByUserId("USER009");

        // Then - 过期优惠券不应出现在可用列表中
        assertThat(availableCoupons).isEmpty();
    }
}
