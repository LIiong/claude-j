package com.claudej.domain.auth.model.aggregate;

import com.claudej.domain.auth.model.valobj.AuthStatus;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.user.model.valobj.UserId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 认证用户聚合根
 */
@Getter
public class AuthUser {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private Long id;
    private UserId userId;
    private String passwordHash;
    private boolean emailVerified;
    private boolean phoneVerified;
    private AuthStatus status;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private AuthUser(UserId userId, String passwordHash, LocalDateTime createTime) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.status = AuthStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.emailVerified = false;
        this.phoneVerified = false;
        this.createTime = createTime;
        this.updateTime = createTime;
    }

    /**
     * 工厂方法：创建新认证用户
     */
    public static AuthUser create(UserId userId, String passwordHash) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_USER_ID, "用户ID不能为空");
        }
        if (passwordHash == null || passwordHash.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码哈希不能为空");
        }
        return new AuthUser(userId, passwordHash, LocalDateTime.now());
    }

    /**
     * 从持久化层重建聚合根
     */
    public static AuthUser reconstruct(Long id, UserId userId, String passwordHash,
                                       boolean emailVerified, boolean phoneVerified,
                                       AuthStatus status, int failedLoginAttempts,
                                       LocalDateTime lockedUntil, LocalDateTime lastLoginAt,
                                       LocalDateTime passwordChangedAt,
                                       LocalDateTime createTime, LocalDateTime updateTime) {
        AuthUser authUser = new AuthUser(userId, passwordHash, createTime);
        authUser.id = id;
        authUser.emailVerified = emailVerified;
        authUser.phoneVerified = phoneVerified;
        authUser.status = status;
        authUser.failedLoginAttempts = failedLoginAttempts;
        authUser.lockedUntil = lockedUntil;
        authUser.lastLoginAt = lastLoginAt;
        authUser.passwordChangedAt = passwordChangedAt;
        authUser.updateTime = updateTime;
        return authUser;
    }

    /**
     * 验证是否可以登录
     */
    public void validateCanLogin() {
        if (status == AuthStatus.DISABLED) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }
        if (status == AuthStatus.LOCKED) {
            if (lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil)) {
                throw new BusinessException(ErrorCode.USER_LOCKED, "用户已被锁定，请" + LOCK_DURATION_MINUTES + "分钟后重试");
            }
            // 锁定时间已过，自动解锁
            unlock();
        }
    }

    /**
     * 记录登录成功
     */
    public void recordLoginSuccess() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 记录登录失败
     */
    public void recordLoginFailure() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.status = AuthStatus.LOCKED;
            this.lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        }
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 解锁用户
     */
    public void unlock() {
        if (this.status == AuthStatus.LOCKED) {
            this.status = AuthStatus.ACTIVE;
            this.failedLoginAttempts = 0;
            this.lockedUntil = null;
            this.updateTime = LocalDateTime.now();
        }
    }

    /**
     * 禁用用户
     */
    public void disable() {
        this.status = AuthStatus.DISABLED;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 启用用户
     */
    public void enable() {
        if (this.status == AuthStatus.DISABLED) {
            this.status = AuthStatus.ACTIVE;
            this.updateTime = LocalDateTime.now();
        }
    }

    /**
     * 修改密码
     */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码哈希不能为空");
        }
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(String passwordHash) {
        return this.passwordHash.equals(passwordHash);
    }

    /**
     * 标记邮箱已验证
     */
    public void markEmailVerified() {
        this.emailVerified = true;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记手机已验证
     */
    public void markPhoneVerified() {
        this.phoneVerified = true;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置数据库自增ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 是否已锁定
     */
    public boolean isLocked() {
        if (status != AuthStatus.LOCKED) {
            return false;
        }
        if (lockedUntil == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * 是否活跃
     */
    public boolean isActive() {
        return status == AuthStatus.ACTIVE;
    }

    /**
     * 获取用户ID字符串值
     */
    public String getUserIdValue() {
        return userId.getValue();
    }
}
