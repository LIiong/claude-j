package com.claudej.domain.common.exception;

public enum ErrorCode {

    INVALID_SHORT_CODE("INVALID_SHORT_CODE", "无效的短链码"),
    INVALID_ORIGINAL_URL("INVALID_ORIGINAL_URL", "无效的原始URL"),
    SHORT_LINK_NOT_FOUND("SHORT_LINK_NOT_FOUND", "短链接不存在"),
    SHORT_CODE_ALREADY_ASSIGNED("SHORT_CODE_ALREADY_ASSIGNED", "短链码已分配"),
    SHORT_LINK_EXPIRED("SHORT_LINK_EXPIRED", "短链接已过期");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
