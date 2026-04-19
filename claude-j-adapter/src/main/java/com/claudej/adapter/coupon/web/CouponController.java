package com.claudej.adapter.coupon.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.coupon.web.request.CreateCouponRequest;
import com.claudej.adapter.coupon.web.request.UseCouponRequest;
import com.claudej.adapter.coupon.web.response.CouponResponse;
import com.claudej.application.coupon.command.CreateCouponCommand;
import com.claudej.application.coupon.command.UseCouponCommand;
import com.claudej.application.coupon.dto.CouponDTO;
import com.claudej.application.coupon.service.CouponApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券 Controller
 */
@Tag(name = "优惠券服务", description = "优惠券创建、领取、使用、查询")
@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponApplicationService couponApplicationService;

    public CouponController(CouponApplicationService couponApplicationService) {
        this.couponApplicationService = couponApplicationService;
    }

    /**
     * 创建优惠券
     */
    @PostMapping
    public ApiResult<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        CreateCouponCommand command = new CreateCouponCommand();
        command.setName(request.getName());
        command.setDiscountType(request.getDiscountType());
        command.setDiscountValue(request.getDiscountValue());
        command.setMinOrderAmount(request.getMinOrderAmount());
        command.setUserId(request.getUserId());
        command.setValidFrom(request.getValidFrom());
        command.setValidUntil(request.getValidUntil());

        CouponDTO dto = couponApplicationService.createCoupon(command);
        CouponResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 根据优惠券ID查询
     */
    @GetMapping("/{couponId}")
    public ApiResult<CouponResponse> getCouponById(@PathVariable String couponId) {
        CouponDTO dto = couponApplicationService.getCouponById(couponId);
        CouponResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 根据用户ID查询优惠券列表
     */
    @GetMapping
    public ApiResult<List<CouponResponse>> getCouponsByUserId(@RequestParam String userId) {
        List<CouponDTO> dtoList = couponApplicationService.getCouponsByUserId(userId);
        List<CouponResponse> responseList = dtoList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResult.ok(responseList);
    }

    /**
     * 根据用户ID查询可用优惠券
     */
    @GetMapping("/available")
    public ApiResult<List<CouponResponse>> getAvailableCouponsByUserId(@RequestParam String userId) {
        List<CouponDTO> dtoList = couponApplicationService.getAvailableCouponsByUserId(userId);
        List<CouponResponse> responseList = dtoList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResult.ok(responseList);
    }

    /**
     * 使用优惠券
     */
    @PostMapping("/{couponId}/use")
    public ApiResult<CouponResponse> useCoupon(@PathVariable String couponId,
                                               @Valid @RequestBody UseCouponRequest request) {
        UseCouponCommand command = new UseCouponCommand();
        command.setOrderId(request.getOrderId());

        CouponDTO dto = couponApplicationService.useCoupon(couponId, command);
        CouponResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    private CouponResponse convertToResponse(CouponDTO dto) {
        CouponResponse response = new CouponResponse();
        response.setCouponId(dto.getCouponId());
        response.setName(dto.getName());
        response.setDiscountType(dto.getDiscountType());
        response.setDiscountValue(dto.getDiscountValue());
        response.setMinOrderAmount(dto.getMinOrderAmount());
        response.setCurrency(dto.getCurrency());
        response.setStatus(dto.getStatus());
        response.setUserId(dto.getUserId());
        response.setValidFrom(dto.getValidFrom());
        response.setValidUntil(dto.getValidUntil());
        response.setUsedTime(dto.getUsedTime());
        response.setUsedOrderId(dto.getUsedOrderId());
        response.setCreateTime(dto.getCreateTime());
        response.setUpdateTime(dto.getUpdateTime());
        return response;
    }
}
