package com.company.common.hub.exception;

/**
 * 認證失敗例外。
 *
 * <p>用於 4 層認證流程中任一層驗證失敗時拋出，
 * 攜帶標準化錯誤代碼和可讀訊息。
 */
public class HubAuthException extends RuntimeException {

    private final String code;

    public HubAuthException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
