package com.claudej.application.payment.assembler;

import com.claudej.application.payment.dto.PaymentDTO;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.payment.model.aggregate.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Payment 转换器
 */
@Mapper(componentModel = "spring")
public interface PaymentAssembler {

    /**
     * Domain 转 DTO
     */
    @Mapping(target = "paymentId", expression = "java(payment.getPaymentIdValue())")
    @Mapping(target = "orderId", expression = "java(payment.getOrderIdValue())")
    @Mapping(target = "customerId", expression = "java(payment.getCustomerIdValue())")
    @Mapping(target = "status", expression = "java(payment.getStatus().name())")
    @Mapping(target = "method", expression = "java(payment.getMethod().name())")
    @Mapping(target = "currency", expression = "java(payment.getAmount().getCurrency())")
    @Mapping(target = "amount", expression = "java(extractAmount(payment.getAmount()))")
    PaymentDTO toDTO(Payment payment);

    /**
     * Domain 列表转 DTO 列表
     */
    List<PaymentDTO> toDTOList(List<Payment> payments);

    /**
     * 金额提取
     */
    default java.math.BigDecimal extractAmount(Money money) {
        return money == null ? null : money.getAmount();
    }
}