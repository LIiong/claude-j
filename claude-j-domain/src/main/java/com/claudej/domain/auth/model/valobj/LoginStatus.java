package com.claudej.domain.auth.model.valobj;

/**
 * 登录状态枚举
 */
public enum LoginStatus {
    SUCCESS("成功"),
    FAILED("失败");

    private final String description;

    LoginStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
