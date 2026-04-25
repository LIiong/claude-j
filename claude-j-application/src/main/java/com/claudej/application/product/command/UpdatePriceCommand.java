package com.claudej.application.product.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调价命令
 */
@Data
public class UpdatePriceCommand {

    private BigDecimal originalPrice;
    private BigDecimal promotionalPrice;
}