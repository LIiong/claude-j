package com.claudej.domain.auth.model.entity;

import com.claudej.domain.auth.model.valobj.AuthProvider;
import com.claudej.domain.auth.model.valobj.DeviceInfo;
import com.claudej.domain.auth.model.valobj.LoginStatus;
import com.claudej.domain.user.model.valobj.UserId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Getter
public class LoginLog {

    private Long id;
    private UserId userId;
    private AuthProvider loginType;
    private String ipAddress;
    private DeviceInfo deviceInfo;
    private LoginStatus status;
    private String failReason;
    private LocalDateTime createTime;

    private LoginLog(AuthProvider loginType, LoginStatus status, LocalDateTime createTime) {
        this.loginType = loginType;
        this.status = status;
        this.createTime = createTime;
    }

    /**
     * 工厂方法：创建登录成功日志
     */
    public static LoginLog createSuccess(UserId userId, AuthProvider loginType,
                                         String ipAddress, DeviceInfo deviceInfo) {
        LoginLog log = new LoginLog(loginType, LoginStatus.SUCCESS, LocalDateTime.now());
        log.userId = userId;
        log.ipAddress = ipAddress;
        log.deviceInfo = deviceInfo;
        return log;
    }

    /**
     * 工厂方法：创建登录失败日志
     */
    public static LoginLog createFailure(UserId userId, AuthProvider loginType,
                                         String ipAddress, DeviceInfo deviceInfo,
                                         String failReason) {
        LoginLog log = new LoginLog(loginType, LoginStatus.FAILED, LocalDateTime.now());
        log.userId = userId;
        log.ipAddress = ipAddress;
        log.deviceInfo = deviceInfo;
        log.failReason = failReason;
        return log;
    }

    /**
     * 从持久化层重建
     */
    public static LoginLog reconstruct(Long id, UserId userId, AuthProvider loginType,
                                       String ipAddress, DeviceInfo deviceInfo,
                                       LoginStatus status, String failReason,
                                       LocalDateTime createTime) {
        LoginLog log = new LoginLog(loginType, status, createTime);
        log.id = id;
        log.userId = userId;
        log.ipAddress = ipAddress;
        log.deviceInfo = deviceInfo;
        log.failReason = failReason;
        return log;
    }

    /**
     * 设置数据库自增ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户ID字符串值
     */
    public String getUserIdValue() {
        return userId != null ? userId.getValue() : null;
    }
}
