package com.company.common.response.code;

/**
 * 錯誤碼介面
 *
 * 各業務模組可實作此介面定義自己的錯誤碼
 *
 * @author Platform Team
 * @version 1.0.0
 */
public interface ErrorCode {

    /**
     * 取得錯誤碼
     */
    String getCode();

    /**
     * 取得錯誤訊息
     */
    String getMessage();

    /**
     * 取得 HTTP 狀態碼
     */
    int getHttpStatus();
}
