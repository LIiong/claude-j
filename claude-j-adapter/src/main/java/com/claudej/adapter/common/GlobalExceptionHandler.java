package com.claudej.adapter.common;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = resolveHttpStatus(ex.getErrorCode());
        ApiResult<Void> result = ApiResult.fail(ex.getErrorCode().getCode(), ex.getMessage());
        return ResponseEntity.status(status).body(result);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("参数校验失败");
        ApiResult<Void> result = ApiResult.fail("VALIDATION_ERROR", message);
        return ResponseEntity.badRequest().body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        ApiResult<Void> result = ApiResult.fail("INTERNAL_ERROR", "系统内部错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        switch (errorCode) {
            case SHORT_LINK_NOT_FOUND:
            case LINK_NOT_FOUND:
            case ORDER_NOT_FOUND:
            case USER_NOT_FOUND:
            case COUPON_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case INVALID_SHORT_CODE:
            case INVALID_ORIGINAL_URL:
            case SHORT_CODE_ALREADY_ASSIGNED:
            case SHORT_LINK_EXPIRED:
            case LINK_NAME_EMPTY:
            case LINK_NAME_TOO_LONG:
            case LINK_URL_EMPTY:
            case LINK_URL_TOO_LONG:
            case LINK_URL_INVALID:
            case ORDER_ITEM_QUANTITY_INVALID:
            case ORDER_ITEM_PRICE_INVALID:
            case INVALID_ORDER_STATUS_TRANSITION:
            case MONEY_AMOUNT_NEGATIVE:
            case MONEY_CURRENCY_EMPTY:
            case COUPON_ID_EMPTY:
            case COUPON_NAME_EMPTY:
            case COUPON_NAME_TOO_LONG:
            case COUPON_DISCOUNT_VALUE_INVALID:
            case COUPON_MIN_ORDER_AMOUNT_INVALID:
            case COUPON_USER_ID_EMPTY:
            case COUPON_VALIDITY_INVALID:
            case COUPON_NOT_YET_VALID:
            case COUPON_ORDER_ID_EMPTY:
            case INVALID_COUPON_STATUS_TRANSITION:
                return HttpStatus.BAD_REQUEST;
            case ORDER_ALREADY_PAID:
            case ORDER_NOT_PAID:
            case ORDER_ALREADY_SHIPPED:
            case ORDER_ALREADY_DELIVERED:
            case ORDER_ALREADY_CANCELLED:
            case ORDER_CANNOT_CANCEL:
                return HttpStatus.CONFLICT;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
