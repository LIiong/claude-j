package com.claudej.adapter.common;

import lombok.Data;

@Data
public class ApiResult<T> {

    private boolean success;
    private T data;
    private String errorCode;
    private String message;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static <T> ApiResult<T> fail(String errorCode, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setMessage(message);
        return result;
    }
}
