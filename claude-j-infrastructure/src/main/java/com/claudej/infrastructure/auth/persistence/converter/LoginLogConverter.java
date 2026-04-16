package com.claudej.infrastructure.auth.persistence.converter;

import com.claudej.domain.auth.model.entity.LoginLog;
import com.claudej.domain.auth.model.valobj.AuthProvider;
import com.claudej.domain.auth.model.valobj.DeviceInfo;
import com.claudej.domain.auth.model.valobj.LoginStatus;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.infrastructure.auth.persistence.dataobject.LoginLogDO;
import org.springframework.stereotype.Component;

/**
 * 登录日志转换器
 */
@Component
public class LoginLogConverter {

    /**
     * LoginLog DO 转 Domain
     */
    public LoginLog toDomain(LoginLogDO loginLogDO) {
        if (loginLogDO == null) {
            return null;
        }

        DeviceInfo deviceInfo = parseDeviceInfo(loginLogDO.getDeviceInfo());
        UserId userId = loginLogDO.getUserId() != null ? new UserId(loginLogDO.getUserId()) : null;

        return LoginLog.reconstruct(
                loginLogDO.getId(),
                userId,
                AuthProvider.valueOf(loginLogDO.getLoginType()),
                loginLogDO.getIpAddress(),
                deviceInfo,
                LoginStatus.valueOf(loginLogDO.getStatus()),
                loginLogDO.getFailReason(),
                loginLogDO.getCreateTime()
        );
    }

    /**
     * LoginLog Domain 转 DO
     */
    public LoginLogDO toDO(LoginLog loginLog) {
        if (loginLog == null) {
            return null;
        }

        LoginLogDO loginLogDO = new LoginLogDO();
        loginLogDO.setId(loginLog.getId());
        loginLogDO.setUserId(loginLog.getUserIdValue());
        loginLogDO.setLoginType(loginLog.getLoginType().name());
        loginLogDO.setIpAddress(loginLog.getIpAddress());
        loginLogDO.setDeviceInfo(serializeDeviceInfo(loginLog.getDeviceInfo()));
        loginLogDO.setStatus(loginLog.getStatus().name());
        loginLogDO.setFailReason(loginLog.getFailReason());
        loginLogDO.setCreateTime(loginLog.getCreateTime());
        loginLogDO.setDeleted(0);
        return loginLogDO;
    }

    /**
     * 解析设备信息
     */
    private DeviceInfo parseDeviceInfo(String deviceInfoJson) {
        if (deviceInfoJson == null || deviceInfoJson.isEmpty()) {
            return null;
        }
        return DeviceInfo.fromUserAgent(deviceInfoJson);
    }

    /**
     * 序列化设备信息
     */
    private String serializeDeviceInfo(DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        return deviceInfo.getUserAgent();
    }
}
