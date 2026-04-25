package com.claudej.adapter.inventory.web.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 调整库存请求（管理员操作）
 */
@Data
public class AdjustStockRequest {

    @NotNull(message = "调整数量不能为空")
    private Integer adjustment;

    private String reason;
}