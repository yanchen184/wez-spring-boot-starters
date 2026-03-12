package com.company.common.response.code;

/**
 * 用戶通用錯誤碼
 *
 * 所有專案共用的用戶相關錯誤碼
 *
 * 錯誤碼規則：USER_Xxxx
 *
 * @author Platform Team
 * @version 1.0.0
 */
public enum UserErrorCode implements ErrorCode {

    // ===== 登入註冊 =====
    USER_NOT_FOUND("USER_B001", "用戶不存在", 404),
    USER_ALREADY_EXISTS("USER_B002", "用戶已存在", 409),
    USERNAME_TAKEN("USER_B003", "用戶名稱已被使用", 409),
    EMAIL_TAKEN("USER_B004", "電子郵件已被使用", 409),
    PHONE_TAKEN("USER_B005", "手機號碼已被使用", 409),

    // ===== 密碼相關 =====
    PASSWORD_INCORRECT("USER_B100", "密碼錯誤", 401),
    PASSWORD_TOO_WEAK("USER_B101", "密碼強度不足", 400),
    PASSWORD_SAME_AS_OLD("USER_B102", "新密碼不可與舊密碼相同", 400),
    PASSWORD_RESET_EXPIRED("USER_B103", "密碼重設連結已過期", 400),

    // ===== 帳號狀態 =====
    ACCOUNT_DISABLED("USER_B200", "帳號已停用", 403),
    ACCOUNT_LOCKED("USER_B201", "帳號已鎖定", 403),
    ACCOUNT_NOT_VERIFIED("USER_B202", "帳號尚未驗證", 403),
    ACCOUNT_EXPIRED("USER_B203", "帳號已過期", 403),

    // ===== 驗證碼 =====
    VERIFICATION_CODE_INVALID("USER_B300", "驗證碼錯誤", 400),
    VERIFICATION_CODE_EXPIRED("USER_B301", "驗證碼已過期", 400),
    VERIFICATION_CODE_SENT_TOO_FREQUENT("USER_B302", "驗證碼發送過於頻繁", 429),

    // ===== 個人資料 =====
    PROFILE_UPDATE_FAILED("USER_B400", "個人資料更新失敗", 400),
    AVATAR_UPLOAD_FAILED("USER_B401", "頭像上傳失敗", 400);

    private final String code;
    private final String message;
    private final int httpStatus;

    UserErrorCode(String code, String message, int httpStatus) {
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
