package com.claudej.adapter.coupon.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.adapter.coupon.web.request.CreateCouponRequest;
import com.claudej.adapter.coupon.web.request.UseCouponRequest;
import com.claudej.application.coupon.dto.CouponDTO;
import com.claudej.application.coupon.service.CouponApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {CouponController.class, GlobalExceptionHandler.class})
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponApplicationService couponApplicationService;

    private CouponDTO mockCouponDTO;

    @BeforeEach
    void setUp() {
        mockCouponDTO = new CouponDTO();
        mockCouponDTO.setCouponId("CP123456");
        mockCouponDTO.setName("满100减20");
        mockCouponDTO.setDiscountType("FIXED_AMOUNT");
        mockCouponDTO.setDiscountValue(new BigDecimal("20.00"));
        mockCouponDTO.setMinOrderAmount(new BigDecimal("100.00"));
        mockCouponDTO.setCurrency("CNY");
        mockCouponDTO.setStatus("AVAILABLE");
        mockCouponDTO.setUserId("USER001");
        mockCouponDTO.setValidFrom(LocalDateTime.now().minusDays(1));
        mockCouponDTO.setValidUntil(LocalDateTime.now().plusDays(30));
        mockCouponDTO.setCreateTime(LocalDateTime.now());
        mockCouponDTO.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void should_return200_when_createCouponSuccess() throws Exception {
        // Given
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("满100减20");
        request.setDiscountType("FIXED_AMOUNT");
        request.setDiscountValue(new BigDecimal("20.00"));
        request.setMinOrderAmount(new BigDecimal("100.00"));
        request.setUserId("USER001");
        request.setValidFrom(LocalDateTime.now().minusDays(1));
        request.setValidUntil(LocalDateTime.now().plusDays(30));

        when(couponApplicationService.createCoupon(any())).thenReturn(mockCouponDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.couponId", is("CP123456")))
                .andExpect(jsonPath("$.data.name", is("满100减20")))
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));
    }

    @Test
    void should_return400_when_createCouponWithInvalidInput() throws Exception {
        // Given
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("");  // Invalid: empty name
        request.setDiscountType("FIXED_AMOUNT");
        request.setDiscountValue(new BigDecimal("20.00"));
        request.setMinOrderAmount(new BigDecimal("100.00"));
        request.setUserId("USER001");
        request.setValidFrom(LocalDateTime.now().minusDays(1));
        request.setValidUntil(LocalDateTime.now().plusDays(30));

        // When & Then
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return200_when_getCouponByIdSuccess() throws Exception {
        // Given
        when(couponApplicationService.getCouponById("CP123456")).thenReturn(mockCouponDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/coupons/CP123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.couponId", is("CP123456")))
                .andExpect(jsonPath("$.data.name", is("满100减20")));
    }

    @Test
    void should_return404_when_getNonExistentCoupon() throws Exception {
        // Given
        when(couponApplicationService.getCouponById("NONEXISTENT"))
                .thenThrow(new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/coupons/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("COUPON_NOT_FOUND")));
    }

    @Test
    void should_return200_when_getCouponsByUserIdSuccess() throws Exception {
        // Given
        when(couponApplicationService.getCouponsByUserId("USER001"))
                .thenReturn(Arrays.asList(mockCouponDTO));

        // When & Then
        mockMvc.perform(get("/api/v1/coupons")
                        .param("userId", "USER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].couponId", is("CP123456")));
    }

    @Test
    void should_return200_when_getAvailableCouponsSuccess() throws Exception {
        // Given
        when(couponApplicationService.getAvailableCouponsByUserId("USER001"))
                .thenReturn(Arrays.asList(mockCouponDTO));

        // When & Then
        mockMvc.perform(get("/api/v1/coupons/available")
                        .param("userId", "USER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].status", is("AVAILABLE")));
    }

    @Test
    void should_return200_when_useCouponSuccess() throws Exception {
        // Given
        UseCouponRequest request = new UseCouponRequest();
        request.setOrderId("ORDER001");

        mockCouponDTO.setStatus("USED");
        mockCouponDTO.setUsedOrderId("ORDER001");
        mockCouponDTO.setUsedTime(LocalDateTime.now());

        when(couponApplicationService.useCoupon(eq("CP123456"), any())).thenReturn(mockCouponDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/coupons/CP123456/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("USED")))
                .andExpect(jsonPath("$.data.usedOrderId", is("ORDER001")));
    }

    @Test
    void should_return400_when_useInvalidCoupon() throws Exception {
        // Given
        UseCouponRequest request = new UseCouponRequest();
        request.setOrderId("ORDER001");

        when(couponApplicationService.useCoupon(eq("CP123456"), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_COUPON_STATUS_TRANSITION));

        // When & Then
        mockMvc.perform(post("/api/v1/coupons/CP123456/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_COUPON_STATUS_TRANSITION")));
    }

    @Test
    void should_return400_when_useCouponWithEmptyOrderId() throws Exception {
        // Given
        UseCouponRequest request = new UseCouponRequest();
        request.setOrderId("");  // Invalid: empty orderId

        // When & Then
        mockMvc.perform(post("/api/v1/coupons/CP123456/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
