package com.claudej.domain.user.model.valobj;

/**
 * 用户角色枚举
 */
public enum Role {

    /**
     * 普通用户角色
     */
    USER,

    /**
     * 管理员角色
     */
    ADMIN;

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 是否为普通用户
     */
    public boolean isUser() {
        return this == USER;
    }
}