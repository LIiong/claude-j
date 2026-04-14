package com.claudej.coupon;

import com.claudej.adapter.coupon.web.request.CreateCouponRequest;
import com.claudej.adapter.coupon.web.request.UseCouponRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 优惠券服务全链路集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CouponIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_createAndQueryCoupon_when_fullFlow() throws Exception {
        // Given - 创建优惠券请求
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("满100减20");
        request.setDiscountType("FIXED_AMOUNT");
        request.setDiscountValue(new BigDecimal("20.00"));
        request.setMinOrderAmount(new BigDecimal("100.00"));
        request.setUserId("USER001");
        request.setValidFrom(LocalDateTime.now().minusDays(1));
        request.setValidUntil(LocalDateTime.now().plusDays(30));

        // When - 创建优惠券
        MvcResult createResult = mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("满100减20")))
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.data.userId", is("USER001")))
                .andReturn();

        // 提取 couponId
        String responseBody = createResult.getResponse().getContentAsString();
        String couponId = objectMapper.readTree(responseBody).path("data").path("couponId").asText();

        // When - 按ID查询
        mockMvc.perform(get("/api/v1/coupons/{couponId}", couponId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.couponId", is(couponId)))
                .andExpect(jsonPath("$.data.name", is("满100减20")));

        // When - 按用户查询
        mockMvc.perform(get("/api/v1/coupons")
                        .param("userId", "USER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].couponId", is(couponId)));

        // When - 查询可用优惠券
        mockMvc.perform(get("/api/v1/coupons/available")
                        .param("userId", "USER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].status", is("AVAILABLE")));
    }

    @Test
    void should_useCouponSuccessfully_when_validRequest() throws Exception {
        // Given - 创建优惠券
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("8折优惠券");
        request.setDiscountType("PERCENTAGE");
        request.setDiscountValue(new BigDecimal("20"));
        request.setMinOrderAmount(new BigDecimal("100.00"));  // @Positive 要求 > 0
        request.setUserId("USER002");
        request.setValidFrom(LocalDateTime.now().minusDays(1));
        request.setValidUntil(LocalDateTime.now().plusDays(30));

        MvcResult createResult = mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String couponId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("couponId").asText();

        // When - 使用优惠券
        UseCouponRequest useRequest = new UseCouponRequest();
        useRequest.setOrderId("ORDER123");

        mockMvc.perform(post("/api/v1/coupons/{couponId}/use", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("USED")))
                .andExpect(jsonPath("$.data.usedOrderId", is("ORDER123")));

        // Then - 再次查询可用优惠券，应该为空（已使用）
        mockMvc.perform(get("/api/v1/coupons/available")
                        .param("userId", "USER002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void should_return404_when_couponNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/coupons/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("COUPON_NOT_FOUND")));
    }

    @Test
    void should_return400_when_createWithInvalidData() throws Exception {
        // Given - 无效请求（名称为空）
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("");  // 空名称
        request.setDiscountType("FIXED_AMOUNT");
        request.setDiscountValue(new BigDecimal("20.00"));
        request.setMinOrderAmount(new BigDecimal("100.00"));
        request.setUserId("USER003");
        request.setValidFrom(LocalDateTime.now().minusDays(1));
        request.setValidUntil(LocalDateTime.now().plusDays(30));

        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_expireCoupon_when_pastValidUntil() throws Exception {
        // Given - 创建即将过期的优惠券（有效期截止到昨天）
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("过期优惠券");
        request.setDiscountType("FIXED_AMOUNT");
        request.setDiscountValue(new BigDecimal("20.00"));
        request.setMinOrderAmount(new BigDecimal("100.00"));  // @Positive 要求 > 0
        request.setUserId("USER004");
        request.setValidFrom(LocalDateTime.now().minusDays(30));
        request.setValidUntil(LocalDateTime.now().minusDays(1));  // 昨天过期

        MvcResult createResult = mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String couponId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("couponId").asText();

        // When - 查询可用优惠券（应该为空，因为已过期）
        mockMvc.perform(get("/api/v1/coupons/available")
                        .param("userId", "USER004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isEmpty());

        // Then - 查询该优惠券，状态应为 EXPIRED
        mockMvc.perform(get("/api/v1/coupons/{couponId}", couponId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("EXPIRED")));
    }
}
