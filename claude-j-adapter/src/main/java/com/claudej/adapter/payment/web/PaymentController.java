package com.claudej.adapter.payment.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.payment.web.request.CreatePaymentRequest;
import com.claudej.adapter.payment.web.request.PaymentCallbackRequest;
import com.claudej.adapter.payment.web.request.RefundPaymentRequest;
import com.claudej.adapter.payment.web.response.PaymentResponse;
import com.claudej.application.payment.command.CreatePaymentCommand;
import com.claudej.application.payment.command.PaymentCallbackCommand;
import com.claudej.application.payment.command.RefundPaymentCommand;
import com.claudej.application.payment.dto.PaymentDTO;
import com.claudej.application.payment.service.PaymentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 支付 Controller
 *
 * 权限说明：
 * - 创建支付：USER
 * - 查询支付：USER
 * - 支付回调：匿名（第三方 PSP 回调）
 * - 退款：ADMIN
 */
@Tag(name = "支付服务", description = "支付创建、查询、回调处理、退款")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    /**
     * 创建支付
     */
    @Operation(summary = "创建支付", description = "为订单创建支付")
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ApiResult<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId(request.getOrderId());
        command.setCustomerId(request.getCustomerId());
        command.setAmount(request.getAmount());
        command.setMethod(request.getMethod());

        PaymentDTO dto = paymentApplicationService.createPayment(command);
        PaymentResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 根据支付ID查询支付详情
     */
    @Operation(summary = "查询支付详情", description = "根据支付ID查询支付详情")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{paymentId}")
    public ApiResult<PaymentResponse> getPaymentById(@PathVariable String paymentId) {
        PaymentDTO dto = paymentApplicationService.getPaymentById(paymentId);
        PaymentResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 根据订单ID查询支付状态
     */
    @Operation(summary = "查询订单支付状态", description = "根据订单ID查询对应的支付状态")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/orders/{orderId}")
    public ApiResult<PaymentResponse> getPaymentByOrderId(@PathVariable String orderId) {
        PaymentDTO dto = paymentApplicationService.getPaymentByOrderId(orderId);
        PaymentResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 支付回调
     */
    @Operation(summary = "支付回调", description = "第三方支付平台回调接口")
    @PostMapping("/callback")
    public ApiResult<PaymentResponse> handleCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        PaymentCallbackCommand command = new PaymentCallbackCommand();
        command.setOrderId(request.getOrderId());
        command.setTransactionNo(request.getTransactionNo());
        command.setSuccess(request.isSuccess());
        command.setMessage(request.getMessage());

        PaymentDTO dto = paymentApplicationService.handleCallback(command);
        PaymentResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 退款
     */
    @Operation(summary = "退款", description = "支付退款操作")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{paymentId}/refund")
    public ApiResult<PaymentResponse> refundPayment(
            @PathVariable String paymentId,
            @RequestBody RefundPaymentRequest request) {
        RefundPaymentCommand command = new RefundPaymentCommand();
        if (request != null) {
            command.setReason(request.getReason());
        }

        PaymentDTO dto = paymentApplicationService.refundPayment(paymentId, command);
        PaymentResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    private PaymentResponse convertToResponse(PaymentDTO dto) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(dto.getPaymentId());
        response.setOrderId(dto.getOrderId());
        response.setCustomerId(dto.getCustomerId());
        response.setAmount(dto.getAmount());
        response.setCurrency(dto.getCurrency());
        response.setStatus(dto.getStatus());
        response.setMethod(dto.getMethod());
        response.setTransactionNo(dto.getTransactionNo());
        response.setCreateTime(dto.getCreateTime());
        response.setUpdateTime(dto.getUpdateTime());
        return response;
    }
}