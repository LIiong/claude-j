package com.claudej.domain.payment.model.valobj;

import lombok.Getter;

/**
 * 支付方式枚举
 */
@Getter
public enum PaymentMethod {

    ALIPAY("支付宝"),
    WECHAT("微信支付"),
    BANK_CARD("银行卡");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }
}