package com.claudej.domain.auth.model.entity;

import com.claudej.domain.auth.model.valobj.DeviceInfo;
import com.claudej.domain.auth.model.valobj.SessionId;
import com.claudej.domain.user.model.valobj.UserId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户会话实体
 */
@Getter
public class UserSession {

    private Long id;
    private SessionId sessionId;
    private UserId userId;
    private String refreshToken;
    private DeviceInfo deviceInfo;
    private String ipAddress;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;

    private UserSession(UserId userId, String refreshToken, LocalDateTime expiresAt, LocalDateTime createTime) {
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.createTime = createTime;
    }

    /**
     * 工厂方法：创建新会话
     */
    public static UserSession create(UserId userId, String refreshToken, LocalDateTime expiresAt) {
        UserSession session = new UserSession(userId, refreshToken, expiresAt, LocalDateTime.now());
        session.sessionId = SessionId.generate();
        return session;
    }

    /**
     * 从持久化层重建
     */
    public static UserSession reconstruct(Long id, SessionId sessionId, UserId userId,
                                          String refreshToken, DeviceInfo deviceInfo,
                                          String ipAddress, LocalDateTime expiresAt,
                                          LocalDateTime createTime) {
        UserSession session = new UserSession(userId, refreshToken, expiresAt, createTime);
        session.id = id;
        session.sessionId = sessionId;
        session.deviceInfo = deviceInfo;
        session.ipAddress = ipAddress;
        return session;
    }

    /**
     * 设置设备信息
     */
    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    /**
     * 设置IP地址
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 设置数据库自增ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 是否已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 获取用户ID字符串值
     */
    public String getUserIdValue() {
        return userId.getValue();
    }

    /**
     * 获取会话ID字符串值
     */
    public String getSessionIdValue() {
        return sessionId.getValue();
    }
}
