package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 用户状态枚举 - 封装状态转换规则
 */
@Getter
public enum UserStatus {

    ACTIVE("活跃"),
    INACTIVE("非活跃"),
    FROZEN("已冻结");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    /**
     * 是否可以冻结
     */
    public boolean canFreeze() {
        return this == ACTIVE || this == INACTIVE;
    }

    /**
     * 是否可以解冻
     */
    public boolean canUnfreeze() {
        return this == FROZEN;
    }

    /**
     * 是否可以激活
     */
    public boolean canActivate() {
        return this == INACTIVE;
    }

    /**
     * 是否可以停用
     */
    public boolean canDeactivate() {
        return this == ACTIVE;
    }

    /**
     * 转换到冻结状态
     */
    public UserStatus toFrozen() {
        if (!canFreeze()) {
            throw new BusinessException(ErrorCode.INVALID_USER_STATUS_TRANSITION,
                    "用户状态 " + this + " 不允许冻结");
        }
        return FROZEN;
    }

    /**
     * 转换到活跃状态
     */
    public UserStatus toActive() {
        if (!canUnfreeze() && !canActivate()) {
            throw new BusinessException(ErrorCode.INVALID_USER_STATUS_TRANSITION,
                    "用户状态 " + this + " 不允许激活");
        }
        return ACTIVE;
    }

    /**
     * 转换到非活跃状态
     */
    public UserStatus toInactive() {
        if (!canDeactivate()) {
            throw new BusinessException(ErrorCode.INVALID_USER_STATUS_TRANSITION,
                    "用户状态 " + this + " 不允许停用");
        }
        return INACTIVE;
    }
}
