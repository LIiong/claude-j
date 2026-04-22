package com.claudej.domain.order.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 订单状态枚举 - 封装状态转换规则
 */
@Getter
public enum OrderStatus {

    CREATED("已创建"),
    PAID("已支付"),
    SHIPPED("已发货"),
    DELIVERED("已送达"),
    CANCELLED("已取消"),
    REFUNDED("已退款");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    /**
     * 是否可以支付
     */
    public boolean canPay() {
        return this == CREATED;
    }

    /**
     * 是否可以发货
     */
    public boolean canShip() {
        return this == PAID;
    }

    /**
     * 是否可以送达
     */
    public boolean canDeliver() {
        return this == SHIPPED;
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == CREATED || this == PAID;
    }

    /**
     * 转换到已支付状态
     */
    public OrderStatus toPaid() {
        if (!canPay()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "订单状态 " + this + " 不允许支付");
        }
        return PAID;
    }

    /**
     * 转换到已发货状态
     */
    public OrderStatus toShipped() {
        if (!canShip()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "订单状态 " + this + " 不允许发货");
        }
        return SHIPPED;
    }

    /**
     * 转换到已送达状态
     */
    public OrderStatus toDelivered() {
        if (!canDeliver()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "订单状态 " + this + " 不允许确认送达");
        }
        return DELIVERED;
    }

    /**
     * 转换到已取消状态
     */
    public OrderStatus toCancelled() {
        if (!canCancel()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "订单状态 " + this + " 不允许取消");
        }
        return CANCELLED;
    }

    /**
     * 是否可以退款
     */
    public boolean canRefund() {
        return this == PAID || this == SHIPPED || this == DELIVERED;
    }

    /**
     * 转换到已退款状态
     */
    public OrderStatus toRefunded() {
        if (!canRefund()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "订单状态 " + this + " 不允许退款");
        }
        return REFUNDED;
    }
}
