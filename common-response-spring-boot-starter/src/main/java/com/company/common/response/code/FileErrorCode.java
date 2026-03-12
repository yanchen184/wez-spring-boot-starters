package com.company.common.response.code;

/**
 * 檔案通用錯誤碼
 *
 * 所有專案共用的檔案上傳/下載相關錯誤碼
 *
 * 錯誤碼規則：FILE_Xxxx
 *
 * @author Platform Team
 * @version 1.0.0
 */
public enum FileErrorCode implements ErrorCode {

    // ===== 上傳相關 =====
    FILE_NOT_FOUND("FILE_B001", "檔案不存在", 404),
    FILE_UPLOAD_FAILED("FILE_B002", "檔案上傳失敗", 500),
    FILE_TOO_LARGE("FILE_B003", "檔案大小超過限制", 400),
    FILE_TYPE_NOT_ALLOWED("FILE_B004", "不允許的檔案類型", 400),
    FILE_NAME_INVALID("FILE_B005", "檔案名稱無效", 400),
    FILE_EMPTY("FILE_B006", "檔案內容為空", 400),

    // ===== 下載相關 =====
    FILE_DOWNLOAD_FAILED("FILE_B100", "檔案下載失敗", 500),
    FILE_ACCESS_DENIED("FILE_B101", "無權存取此檔案", 403),
    FILE_EXPIRED("FILE_B102", "檔案已過期", 410),

    // ===== 儲存相關 =====
    STORAGE_FULL("FILE_C001", "儲存空間已滿", 507),
    STORAGE_ERROR("FILE_C002", "儲存服務異常", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    FileErrorCode(String code, String message, int httpStatus) {
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
