package com.company.common.response.code;

/**
 * 訂單通用錯誤碼
 *
 * 所有專案共用的訂單相關錯誤碼
 * 業務專案可以直接使用或擴展
 *
 * 錯誤碼規則：ORDER_Xxxx
 * - ORDER_A: 訂單參數錯誤
 * - ORDER_B: 訂單業務錯誤
 * - ORDER_C: 訂單系統錯誤
 *
 * @author Platform Team
 * @version 1.0.0
 */
public enum OrderErrorCode implements ErrorCode {

    // ===== ORDER_A 訂單參數錯誤 =====
    ORDER_INVALID_PARAM("ORDER_A001", "訂單參數錯誤", 400),
    ORDER_ITEM_EMPTY("ORDER_A002", "訂單商品不可為空", 400),
    ORDER_AMOUNT_INVALID("ORDER_A003", "訂單金額不正確", 400),

    // ===== ORDER_B 訂單業務錯誤 =====
    ORDER_NOT_FOUND("ORDER_B001", "訂單不存在", 404),
    ORDER_ALREADY_PAID("ORDER_B002", "訂單已支付，無法重複支付", 400),
    ORDER_CANCELLED("ORDER_B003", "訂單已取消", 400),
    ORDER_EXPIRED("ORDER_B004", "訂單已過期", 400),
    ORDER_STATUS_INVALID("ORDER_B005", "訂單狀態不正確", 400),
    ORDER_CANNOT_CANCEL("ORDER_B006", "訂單無法取消", 400),
    ORDER_CANNOT_MODIFY("ORDER_B007", "訂單無法修改", 400),
    ORDER_REFUND_FAILED("ORDER_B008", "訂單退款失敗", 400),

    // ===== 支付相關 =====
    PAYMENT_FAILED("ORDER_B100", "支付失敗", 400),
    PAYMENT_AMOUNT_MISMATCH("ORDER_B101", "支付金額不符", 400),
    PAYMENT_METHOD_NOT_SUPPORTED("ORDER_B102", "不支援的支付方式", 400),
    PAYMENT_TIMEOUT("ORDER_B103", "支付逾時", 400),

    // ===== 庫存相關 =====
    PRODUCT_NOT_FOUND("ORDER_B200", "商品不存在", 404),
    PRODUCT_OUT_OF_STOCK("ORDER_B201", "商品已售完", 400),
    INSUFFICIENT_STOCK("ORDER_B202", "庫存不足", 400),
    PRODUCT_OFF_SHELF("ORDER_B203", "商品已下架", 400),
    PRODUCT_PRICE_CHANGED("ORDER_B204", "商品價格已變動", 400),

    // ===== 配送相關 =====
    SHIPPING_ADDRESS_INVALID("ORDER_B300", "配送地址無效", 400),
    SHIPPING_METHOD_UNAVAILABLE("ORDER_B301", "配送方式不可用", 400),
    SHIPPING_AREA_NOT_SUPPORTED("ORDER_B302", "不支援配送到此地區", 400);

    private final String code;
    private final String message;
    private final int httpStatus;

    OrderErrorCode(String code, String message, int httpStatus) {
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
