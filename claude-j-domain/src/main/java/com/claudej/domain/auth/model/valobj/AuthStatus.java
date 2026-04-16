package com.claudej.domain.auth.model.valobj;

/**
 * 认证状态枚举
 */
public enum AuthStatus {
    ACTIVE("正常"),
    LOCKED("已锁定"),
    DISABLED("已禁用");

    private final String description;

    AuthStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否可以登录
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }

    /**
     * 转换为锁定状态
     */
    public AuthStatus toLocked() {
        if (this == DISABLED) {
            throw new IllegalStateException("已禁用用户不能锁定");
        }
        return LOCKED;
    }

    /**
     * 转换为正常状态
     */
    public AuthStatus toActive() {
        if (this == DISABLED) {
            throw new IllegalStateException("已禁用用户不能激活");
        }
        return ACTIVE;
    }

    /**
     * 转换为禁用状态
     */
    public AuthStatus toDisabled() {
        return DISABLED;
    }
}
