package com.claudej.application.coupon.service;

import com.claudej.application.coupon.assembler.CouponAssembler;
import com.claudej.application.coupon.command.CreateCouponCommand;
import com.claudej.application.coupon.command.UseCouponCommand;
import com.claudej.application.coupon.dto.CouponDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.model.valobj.DiscountType;
import com.claudej.domain.coupon.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponApplicationServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponAssembler couponAssembler;

    @InjectMocks
    private CouponApplicationService couponApplicationService;

    private CreateCouponCommand createCommand;
    private Coupon mockCoupon;
    private CouponDTO mockCouponDTO;

    @BeforeEach
    void setUp() {
        createCommand = new CreateCouponCommand();
        createCommand.setName("满100减20");
        createCommand.setDiscountType("FIXED_AMOUNT");
        createCommand.setDiscountValue(new BigDecimal("20.00"));
        createCommand.setMinOrderAmount(new BigDecimal("100.00"));
        createCommand.setUserId("USER001");
        createCommand.setValidFrom(LocalDateTime.now().minusDays(1));
        createCommand.setValidUntil(LocalDateTime.now().plusDays(30));

        mockCoupon = Coupon.create(
                "满100减20",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "USER001",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        mockCouponDTO = new CouponDTO();
        mockCouponDTO.setCouponId("CP123456");
        mockCouponDTO.setName("满100减20");
        mockCouponDTO.setDiscountType("FIXED_AMOUNT");
        mockCouponDTO.setDiscountValue(new BigDecimal("20.00"));
        mockCouponDTO.setStatus("AVAILABLE");
        mockCouponDTO.setUserId("USER001");
    }

    @Test
    void should_createCoupon_when_validCommandProvided() {
        // Given
        when(couponRepository.save(any(Coupon.class))).thenReturn(mockCoupon);
        when(couponAssembler.toDTO(any(Coupon.class))).thenReturn(mockCouponDTO);

        // When
        CouponDTO result = couponApplicationService.createCoupon(createCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("满100减20");
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void should_throwException_when_createCouponWithNullCommand() {
        // When & Then
        assertThatThrownBy(() -> couponApplicationService.createCoupon(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("命令不能为空");
    }

    @Test
    void should_returnCoupon_when_getCouponByIdExists() {
        // Given
        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(mockCoupon));
        when(couponAssembler.toDTO(any(Coupon.class))).thenReturn(mockCouponDTO);

        // When
        CouponDTO result = couponApplicationService.getCouponById("CP123456");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCouponId()).isEqualTo("CP123456");
    }

    @Test
    void should_throwException_when_getCouponByIdNotExists() {
        // Given
        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponApplicationService.getCouponById("NONEXISTENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("优惠券不存在");
    }

    @Test
    void should_returnCoupons_when_getCouponsByUserId() {
        // Given
        when(couponRepository.findByUserId("USER001")).thenReturn(Arrays.asList(mockCoupon));
        when(couponAssembler.toDTOList(any())).thenReturn(Arrays.asList(mockCouponDTO));

        // When
        java.util.List<CouponDTO> results = couponApplicationService.getCouponsByUserId("USER001");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUserId()).isEqualTo("USER001");
    }

    @Test
    void should_returnAvailableCoupons_when_getAvailableCouponsByUserId() {
        // Given
        when(couponRepository.findAvailableByUserId("USER001")).thenReturn(Arrays.asList(mockCoupon));
        when(couponAssembler.toDTOList(any())).thenReturn(Arrays.asList(mockCouponDTO));

        // When
        java.util.List<CouponDTO> results = couponApplicationService.getAvailableCouponsByUserId("USER001");

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void should_useCoupon_when_couponExistsAndAvailable() {
        // Given
        UseCouponCommand useCommand = new UseCouponCommand();
        useCommand.setOrderId("ORDER001");

        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(mockCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(mockCoupon);
        when(couponAssembler.toDTO(any(Coupon.class))).thenReturn(mockCouponDTO);

        // When
        CouponDTO result = couponApplicationService.useCoupon("CP123456", useCommand);

        // Then
        assertThat(result).isNotNull();
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void should_throwException_when_useCouponWithEmptyOrderId() {
        // Given
        UseCouponCommand useCommand = new UseCouponCommand();
        useCommand.setOrderId(null);

        // When & Then
        assertThatThrownBy(() -> couponApplicationService.useCoupon("CP123456", useCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单号不能为空");
    }
}
