package com.claudej.infrastructure.payment.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付数据对象
 */
@Data
@TableName("t_payment")
public class PaymentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentId;

    private String orderId;

    private String customerId;

    private BigDecimal amount;

    private String currency;

    private String status;

    private String method;

    private String transactionNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}