package com.claudej.infrastructure.payment.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentId;
import com.claudej.domain.payment.repository.PaymentRepository;
import com.claudej.infrastructure.payment.persistence.converter.PaymentConverter;
import com.claudej.infrastructure.payment.persistence.dataobject.PaymentDO;
import com.claudej.infrastructure.payment.persistence.mapper.PaymentMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 支付 Repository 实现
 */
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentMapper paymentMapper;
    private final PaymentConverter paymentConverter;

    public PaymentRepositoryImpl(PaymentMapper paymentMapper, PaymentConverter paymentConverter) {
        this.paymentMapper = paymentMapper;
        this.paymentConverter = paymentConverter;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentDO paymentDO = paymentConverter.toDO(payment);
        if (payment.getId() == null) {
            // 新增支付
            paymentDO.setCreateTime(LocalDateTime.now());
            paymentDO.setUpdateTime(LocalDateTime.now());
            paymentDO.setDeleted(0);
            paymentMapper.insert(paymentDO);
            payment.setId(paymentDO.getId());
        } else {
            // 更新支付
            paymentDO.setUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(paymentDO);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findByPaymentId(PaymentId paymentId) {
        LambdaQueryWrapper<PaymentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentDO::getPaymentId, paymentId.getValue());
        PaymentDO paymentDO = paymentMapper.selectOne(wrapper);

        if (paymentDO == null) {
            return Optional.empty();
        }

        Payment payment = paymentConverter.toDomain(paymentDO);
        return Optional.ofNullable(payment);
    }

    @Override
    public Optional<Payment> findByOrderId(OrderId orderId) {
        LambdaQueryWrapper<PaymentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentDO::getOrderId, orderId.getValue());
        PaymentDO paymentDO = paymentMapper.selectOne(wrapper);

        if (paymentDO == null) {
            return Optional.empty();
        }

        Payment payment = paymentConverter.toDomain(paymentDO);
        return Optional.ofNullable(payment);
    }

    @Override
    public boolean existsByPaymentId(PaymentId paymentId) {
        LambdaQueryWrapper<PaymentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentDO::getPaymentId, paymentId.getValue());
        return paymentMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Payment> findByCustomerId(CustomerId customerId) {
        LambdaQueryWrapper<PaymentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentDO::getCustomerId, customerId.getValue());
        List<PaymentDO> paymentDOList = paymentMapper.selectList(wrapper);

        return paymentDOList.stream()
                .map(paymentConverter::toDomain)
                .collect(Collectors.toList());
    }
}