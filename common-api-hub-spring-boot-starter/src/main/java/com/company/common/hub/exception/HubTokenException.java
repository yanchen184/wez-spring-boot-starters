package com.company.common.hub.exception;

/**
 * JWT Token 相關例外。
 *
 * <p>錯誤代碼：
 * <ul>
 *   <li>{@code 422001} — Token 解密失敗（被竄改或無效）</li>
 *   <li>{@code 422002} — Token 已過期</li>
 * </ul>
 */
public class HubTokenException extends RuntimeException {

    private final String code;

    public HubTokenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public HubTokenException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
