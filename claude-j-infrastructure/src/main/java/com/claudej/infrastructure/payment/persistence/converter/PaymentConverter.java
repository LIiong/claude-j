package com.claudej.infrastructure.payment.persistence.converter;

import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentId;
import com.claudej.domain.payment.model.valobj.PaymentMethod;
import com.claudej.domain.payment.model.valobj.PaymentStatus;
import com.claudej.infrastructure.payment.persistence.dataobject.PaymentDO;
import org.springframework.stereotype.Component;

/**
 * Payment 转换器
 */
@Component
public class PaymentConverter {

    /**
     * Payment DO 转 Domain
     */
    public Payment toDomain(PaymentDO paymentDO) {
        if (paymentDO == null) {
            return null;
        }

        Money amount = new Money(paymentDO.getAmount(), paymentDO.getCurrency());
        PaymentStatus status = PaymentStatus.valueOf(paymentDO.getStatus());
        PaymentMethod method = PaymentMethod.valueOf(paymentDO.getMethod());

        return Payment.reconstruct(
                paymentDO.getId(),
                new PaymentId(paymentDO.getPaymentId()),
                new OrderId(paymentDO.getOrderId()),
                new CustomerId(paymentDO.getCustomerId()),
                amount,
                status,
                method,
                paymentDO.getTransactionNo(),
                paymentDO.getCreateTime(),
                paymentDO.getUpdateTime()
        );
    }

    /**
     * Payment Domain 转 DO
     */
    public PaymentDO toDO(Payment payment) {
        if (payment == null) {
            return null;
        }
        PaymentDO paymentDO = new PaymentDO();
        paymentDO.setId(payment.getId());
        paymentDO.setPaymentId(payment.getPaymentIdValue());
        paymentDO.setOrderId(payment.getOrderIdValue());
        paymentDO.setCustomerId(payment.getCustomerIdValue());
        paymentDO.setAmount(payment.getAmount().getAmount());
        paymentDO.setCurrency(payment.getAmount().getCurrency());
        paymentDO.setStatus(payment.getStatus().name());
        paymentDO.setMethod(payment.getMethod().name());
        paymentDO.setTransactionNo(payment.getTransactionNo());
        paymentDO.setCreateTime(payment.getCreateTime());
        paymentDO.setUpdateTime(payment.getUpdateTime());
        paymentDO.setDeleted(0);
        return paymentDO;
    }
}