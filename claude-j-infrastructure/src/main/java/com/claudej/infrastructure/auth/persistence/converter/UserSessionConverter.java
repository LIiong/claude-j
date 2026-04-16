package com.claudej.infrastructure.auth.persistence.converter;

import com.claudej.domain.auth.model.entity.UserSession;
import com.claudej.domain.auth.model.valobj.DeviceInfo;
import com.claudej.domain.auth.model.valobj.SessionId;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.infrastructure.auth.persistence.dataobject.UserSessionDO;
import org.springframework.stereotype.Component;

/**
 * 用户会话转换器
 */
@Component
public class UserSessionConverter {

    /**
     * UserSession DO 转 Domain
     */
    public UserSession toDomain(UserSessionDO userSessionDO) {
        if (userSessionDO == null) {
            return null;
        }

        DeviceInfo deviceInfo = parseDeviceInfo(userSessionDO.getDeviceInfo());

        return UserSession.reconstruct(
                userSessionDO.getId(),
                SessionId.of(userSessionDO.getSessionId()),
                new UserId(userSessionDO.getUserId()),
                userSessionDO.getRefreshToken(),
                deviceInfo,
                userSessionDO.getIpAddress(),
                userSessionDO.getExpiresAt(),
                userSessionDO.getCreateTime()
        );
    }

    /**
     * UserSession Domain 转 DO
     */
    public UserSessionDO toDO(UserSession userSession) {
        if (userSession == null) {
            return null;
        }

        UserSessionDO userSessionDO = new UserSessionDO();
        userSessionDO.setId(userSession.getId());
        userSessionDO.setSessionId(userSession.getSessionIdValue());
        userSessionDO.setUserId(userSession.getUserIdValue());
        userSessionDO.setRefreshToken(userSession.getRefreshToken());
        userSessionDO.setDeviceInfo(serializeDeviceInfo(userSession.getDeviceInfo()));
        userSessionDO.setIpAddress(userSession.getIpAddress());
        userSessionDO.setExpiresAt(userSession.getExpiresAt());
        userSessionDO.setCreateTime(userSession.getCreateTime());
        userSessionDO.setDeleted(0);
        return userSessionDO;
    }

    /**
     * 解析设备信息JSON
     */
    private DeviceInfo parseDeviceInfo(String deviceInfoJson) {
        if (deviceInfoJson == null || deviceInfoJson.isEmpty()) {
            return null;
        }
        // 简化实现：直接存储User-Agent，后续可扩展为JSON解析
        return DeviceInfo.fromUserAgent(deviceInfoJson);
    }

    /**
     * 序列化设备信息为JSON
     */
    private String serializeDeviceInfo(DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        // 简化实现：存储User-Agent
        return deviceInfo.getUserAgent();
    }
}
