package com.claudej.domain.auth.model.valobj;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 设备信息值对象
 */
@Getter
@EqualsAndHashCode
public class DeviceInfo {

    private final String userAgent;
    private final String deviceType;
    private final String os;
    private final String browser;

    private DeviceInfo(String userAgent, String deviceType, String os, String browser) {
        this.userAgent = userAgent;
        this.deviceType = deviceType;
        this.os = os;
        this.browser = browser;
    }

    /**
     * 创建设备信息
     */
    public static DeviceInfo of(String userAgent, String deviceType, String os, String browser) {
        return new DeviceInfo(userAgent, deviceType, os, browser);
    }

    /**
     * 从User-Agent解析（简化版）
     */
    public static DeviceInfo fromUserAgent(String userAgent) {
        if (userAgent == null) {
            return new DeviceInfo(null, "unknown", "unknown", "unknown");
        }
        String deviceType = userAgent.contains("Mobile") ? "mobile" : "desktop";
        String os = "unknown";
        String browser = "unknown";

        if (userAgent.contains("Windows")) {
            os = "Windows";
        } else if (userAgent.contains("Mac")) {
            os = "MacOS";
        } else if (userAgent.contains("Linux")) {
            os = "Linux";
        }

        if (userAgent.contains("Chrome")) {
            browser = "Chrome";
        } else if (userAgent.contains("Firefox")) {
            browser = "Firefox";
        } else if (userAgent.contains("Safari")) {
            browser = "Safari";
        }

        return new DeviceInfo(userAgent, deviceType, os, browser);
    }
}
