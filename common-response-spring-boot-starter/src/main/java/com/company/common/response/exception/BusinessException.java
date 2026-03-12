package com.company.common.response.exception;

import com.company.common.response.code.CommonErrorCode;
import com.company.common.response.code.ErrorCode;

/**
 * 業務異常
 *
 * 所有業務邏輯相關的異常都應該拋出此類
 * GlobalExceptionHandler 會統一處理並返回標準格式
 *
 * 使用範例：
 * <pre>
 * // 使用預定義錯誤碼
 * throw new BusinessException(CommonErrorCode.INSUFFICIENT_BALANCE);
 *
 * // 使用自訂訊息
 * throw new BusinessException(CommonErrorCode.BUSINESS_ERROR, "訂單已取消，無法修改");
 *
 * // 使用自訂錯誤碼
 * throw new BusinessException("ORDER_001", "訂單不存在");
 * </pre>
 *
 * @author Platform Team
 * @version 1.0.0
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = 400;
    }

    public BusinessException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    // ===== 靜態工廠方法（語意化）=====

    public static BusinessException notFound(String message) {
        return new BusinessException(CommonErrorCode.NOT_FOUND, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(CommonErrorCode.UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(CommonErrorCode.FORBIDDEN, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(CommonErrorCode.CONFLICT, message);
    }

    public static BusinessException internal(String message) {
        return new BusinessException(CommonErrorCode.INTERNAL_ERROR, message);
    }
}
