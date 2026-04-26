package com.claudej.infrastructure.payment.gateway;

import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentResult;
import com.claudej.domain.payment.service.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock 支付网关实现 - 测试环境模拟
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private boolean simulateSuccess = true;
    private String simulateMessage = "支付成功";

    /**
     * 设置模拟结果
     */
    public void setSimulateSuccess(boolean success, String message) {
        this.simulateSuccess = success;
        this.simulateMessage = message;
    }

    /**
     * 重置为默认状态
     */
    public void reset() {
        this.simulateSuccess = true;
        this.simulateMessage = "支付成功";
    }

    @Override
    public PaymentResult createPayment(Payment payment) {
        if (simulateSuccess) {
            String transactionNo = "MOCK_TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            return PaymentResult.success(transactionNo);
        } else {
            return PaymentResult.failed(simulateMessage);
        }
    }

    @Override
    public PaymentResult queryPayment(String transactionNo) {
        if (simulateSuccess) {
            return PaymentResult.success(transactionNo);
        } else {
            return PaymentResult.failed(simulateMessage);
        }
    }

    @Override
    public PaymentResult refundPayment(String transactionNo, Money amount) {
        if (simulateSuccess) {
            String refundNo = "MOCK_REFUND_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            return PaymentResult.success(refundNo);
        } else {
            return PaymentResult.failed("退款失败: " + simulateMessage);
        }
    }
}