package com.claudej.domain.auth.model.valobj;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * 会话ID值对象
 */
@Getter
@EqualsAndHashCode
public class SessionId {

    private final String value;

    private SessionId(String value) {
        this.value = value;
    }

    /**
     * 生成新的会话ID
     */
    public static SessionId generate() {
        return new SessionId(UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 从字符串重建
     */
    public static SessionId of(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        return new SessionId(value);
    }
}
