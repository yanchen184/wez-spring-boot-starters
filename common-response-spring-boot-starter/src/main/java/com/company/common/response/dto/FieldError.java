package com.company.common.response.dto;

/**
 * 欄位驗證錯誤
 *
 * @author Platform Team
 * @version 1.0.0
 */
public class FieldError {

    /**
     * 欄位名稱
     */
    private String field;

    /**
     * 錯誤訊息
     */
    private String message;

    /**
     * 拒絕的值
     */
    private Object rejectedValue;

    public FieldError() {
    }

    public FieldError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public FieldError(String field, String message, Object rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }

    // Static factory
    public static FieldError of(String field, String message) {
        return new FieldError(field, message);
    }

    public static FieldError of(String field, String message, Object rejectedValue) {
        return new FieldError(field, message, rejectedValue);
    }

    // Getters and Setters

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public void setRejectedValue(Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }
}
