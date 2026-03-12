package com.company.common.response.code;

/**
 * 通用錯誤碼
 *
 * 所有專案共用的基礎錯誤碼
 * 業務模組可以繼承或另外定義
 *
 * 錯誤碼規則：
 * - SUCCESS: 成功
 * - A0xxx: 用戶端錯誤（參數、權限等）
 * - B0xxx: 業務錯誤
 * - C0xxx: 系統錯誤（內部異常）
 * - D0xxx: 外部服務錯誤
 *
 * @author Platform Team
 * @version 1.0.0
 */
public enum CommonErrorCode implements ErrorCode {

    // ===== 成功 =====
    SUCCESS("SUCCESS", "操作成功", 200),

    // ===== A0xxx 用戶端錯誤 =====
    BAD_REQUEST("A0001", "請求參數錯誤", 400),
    VALIDATION_ERROR("A0002", "驗證失敗", 400),
    INVALID_PARAMETER("A0003", "無效的參數", 400),
    MISSING_PARAMETER("A0004", "缺少必要參數", 400),

    UNAUTHORIZED("A0100", "未授權，請先登入", 401),
    INVALID_TOKEN("A0101", "無效的 Token", 401),
    TOKEN_EXPIRED("A0102", "Token 已過期", 401),

    FORBIDDEN("A0200", "無權限執行此操作", 403),
    ACCESS_DENIED("A0201", "存取被拒絕", 403),

    NOT_FOUND("A0300", "資源不存在", 404),
    USER_NOT_FOUND("A0301", "用戶不存在", 404),
    RESOURCE_NOT_FOUND("A0302", "請求的資源不存在", 404),

    METHOD_NOT_ALLOWED("A0400", "不支援的請求方法", 405),

    CONFLICT("A0500", "資源衝突", 409),
    DUPLICATE_ENTRY("A0501", "資料重複", 409),

    TOO_MANY_REQUESTS("A0600", "請求過於頻繁，請稍後再試", 429),

    // ===== B0xxx 業務錯誤 =====
    BUSINESS_ERROR("B0001", "業務處理失敗", 400),
    OPERATION_FAILED("B0002", "操作失敗", 400),
    INVALID_STATE("B0003", "無效的狀態", 400),
    INSUFFICIENT_BALANCE("B0100", "餘額不足", 400),
    INVENTORY_SHORTAGE("B0101", "庫存不足", 400),

    // ===== C0xxx 系統錯誤 =====
    INTERNAL_ERROR("C0001", "系統內部錯誤", 500),
    SERVICE_UNAVAILABLE("C0002", "服務暫時不可用", 503),
    DATABASE_ERROR("C0100", "資料庫錯誤", 500),
    CACHE_ERROR("C0101", "快取錯誤", 500),

    // ===== D0xxx 外部服務錯誤 =====
    EXTERNAL_SERVICE_ERROR("D0001", "外部服務呼叫失敗", 502),
    GATEWAY_TIMEOUT("D0002", "閘道逾時", 504),
    THIRD_PARTY_ERROR("D0003", "第三方服務異常", 502);

    private final String code;
    private final String message;
    private final int httpStatus;

    CommonErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}
