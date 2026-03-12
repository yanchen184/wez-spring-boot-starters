package com.company.common.response.dto;

import com.company.common.response.code.CommonErrorCode;
import com.company.common.response.code.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 統一 API 回應封裝
 *
 * @param <T> 資料類型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private List<FieldError> errors;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ===== 成功 =====

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("Success", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, CommonErrorCode.SUCCESS.getCode(), message, data);
    }

    // ===== 失敗 =====

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, CommonErrorCode.BAD_REQUEST.getCode(), message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    // ===== 驗證錯誤 =====

    public static <T> ApiResponse<T> validationError(List<FieldError> errors) {
        ApiResponse<T> response = new ApiResponse<>(
                false, CommonErrorCode.VALIDATION_ERROR.getCode(),
                CommonErrorCode.VALIDATION_ERROR.getMessage(), null);
        response.setErrors(errors);
        return response;
    }

    // ===== Getters and Setters =====

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldError> errors) {
        this.errors = errors;
    }
}
