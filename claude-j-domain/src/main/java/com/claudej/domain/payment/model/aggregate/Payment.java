package com.claudej.domain.payment.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.payment.model.valobj.PaymentId;
import com.claudej.domain.payment.model.valobj.PaymentMethod;
import com.claudej.domain.payment.model.valobj.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付聚合根 - 封装支付业务不变量
 *
 * 状态机：
 * PENDING -> SUCCESS (支付成功回调)
 * PENDING -> FAILED (支付失败回调)
 * SUCCESS -> REFUNDED (管理员退款)
 */
@Getter
public class Payment {

    private Long id;
    private PaymentId paymentId;
    private OrderId orderId;
    private CustomerId customerId;
    private Money amount;
    private PaymentStatus status;
    private PaymentMethod method;
    private String transactionNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Payment(OrderId orderId, CustomerId customerId, Money amount, PaymentMethod method, LocalDateTime createTime) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.PENDING;
        this.createTime = createTime;
        this.updateTime = createTime;
    }

    /**
     * 工厂方法：创建新支付
     */
    public static Payment create(OrderId orderId, CustomerId customerId, Money amount, PaymentMethod method) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_ID_EMPTY, "订单ID不能为空");
        }
        if (customerId == null) {
            throw new BusinessException(ErrorCode.PAYMENT_CUSTOMER_ID_EMPTY, "客户ID不能为空");
        }
        if (amount == null || amount.isZero()) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_INVALID, "支付金额无效");
        }
        if (method == null) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_INVALID, "支付方式无效");
        }

        Payment payment = new Payment(orderId, customerId, amount, method, LocalDateTime.now());
        payment.paymentId = new PaymentId(UUID.randomUUID().toString().replace("-", ""));
        return payment;
    }

    /**
     * 从持久化层重建聚合根
     */
    public static Payment reconstruct(Long id, PaymentId paymentId, OrderId orderId, CustomerId customerId,
                                       Money amount, PaymentStatus status, PaymentMethod method,
                                       String transactionNo, LocalDateTime createTime, LocalDateTime updateTime) {
        Payment payment = new Payment(orderId, customerId, amount, method, createTime);
        payment.id = id;
        payment.paymentId = paymentId;
        payment.status = status;
        payment.transactionNo = transactionNo;
        payment.updateTime = updateTime;
        return payment;
    }

    /**
     * 标记为支付成功
     */
    public void markAsSuccess(String transactionNo) {
        if (transactionNo == null || transactionNo.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "交易号不能为空");
        }
        this.status = this.status.toSuccess();
        this.transactionNo = transactionNo.trim();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为支付失败
     */
    public void markAsFailed(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "失败消息不能为空");
        }
        this.status = this.status.toFailed();
        this.transactionNo = null;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 退款
     */
    public void refund() {
        this.status = this.status.toRefunded();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置数据库自增 ID（持久化后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 便捷获取支付ID字符串值
     */
    public String getPaymentIdValue() {
        return paymentId.getValue();
    }

    /**
     * 便捷获取订单ID字符串值
     */
    public String getOrderIdValue() {
        return orderId.getValue();
    }

    /**
     * 便捷获取客户ID字符串值
     */
    public String getCustomerIdValue() {
        return customerId.getValue();
    }

    /**
     * 是否为终态
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }
}