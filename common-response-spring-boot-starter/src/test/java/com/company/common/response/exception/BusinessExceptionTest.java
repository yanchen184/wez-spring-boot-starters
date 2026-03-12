package com.company.common.response.exception;

import com.company.common.response.code.CommonErrorCode;
import com.company.common.response.code.UserErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BusinessException 業務異常驗證
 *
 * 確保各種建構方式正確帶出 code、message、httpStatus。
 */
@DisplayName("BusinessException 業務異常")
class BusinessExceptionTest {

    @Nested
    @DisplayName("使用 ErrorCode 建構")
    class WithErrorCode {

        @Test
        @DisplayName("從 ErrorCode 建構時，取用 ErrorCode 的 code/message/httpStatus")
        void 從ErrorCode建構() {
            BusinessException ex = new BusinessException(CommonErrorCode.NOT_FOUND);

            assertThat(ex.getCode()).isEqualTo("A0300");
            assertThat(ex.getMessage()).isEqualTo("資源不存在");
            assertThat(ex.getHttpStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("從 ErrorCode 建構可覆蓋 message")
        void 從ErrorCode建構可覆蓋message() {
            BusinessException ex = new BusinessException(CommonErrorCode.NOT_FOUND, "找不到訂單 #123");

            assertThat(ex.getCode()).isEqualTo("A0300");
            assertThat(ex.getMessage()).isEqualTo("找不到訂單 #123");
            assertThat(ex.getHttpStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("從 ErrorCode 建構可帶原始異常 cause")
        void 從ErrorCode建構可帶cause() {
            RuntimeException cause = new RuntimeException("root cause");
            BusinessException ex = new BusinessException(CommonErrorCode.INTERNAL_ERROR, cause);

            assertThat(ex.getCode()).isEqualTo("C0001");
            assertThat(ex.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("使用自訂 code 字串建構")
    class WithCustomCode {

        @Test
        @DisplayName("自訂 code + message 時，httpStatus 預設 400")
        void 自訂code時httpStatus預設400() {
            BusinessException ex = new BusinessException("ORDER_001", "訂單不存在");

            assertThat(ex.getCode()).isEqualTo("ORDER_001");
            assertThat(ex.getMessage()).isEqualTo("訂單不存在");
            assertThat(ex.getHttpStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("自訂 code + message + httpStatus")
        void 自訂三個參數() {
            BusinessException ex = new BusinessException("CUSTOM_500", "伺服器忙碌", 503);

            assertThat(ex.getHttpStatus()).isEqualTo(503);
        }
    }

    @Nested
    @DisplayName("靜態工廠方法 — 語意化建構")
    class StaticFactoryMethods {

        @Test
        @DisplayName("notFound 回傳 404")
        void notFound回傳404() {
            BusinessException ex = BusinessException.notFound("找不到用戶");

            assertThat(ex.getCode()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode());
            assertThat(ex.getHttpStatus()).isEqualTo(404);
            assertThat(ex.getMessage()).isEqualTo("找不到用戶");
        }

        @Test
        @DisplayName("unauthorized 回傳 401")
        void unauthorized回傳401() {
            BusinessException ex = BusinessException.unauthorized("請先登入");

            assertThat(ex.getHttpStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("forbidden 回傳 403")
        void forbidden回傳403() {
            BusinessException ex = BusinessException.forbidden("無權限");

            assertThat(ex.getHttpStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("badRequest 回傳 400")
        void badRequest回傳400() {
            BusinessException ex = BusinessException.badRequest("參數錯誤");

            assertThat(ex.getHttpStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("conflict 回傳 409")
        void conflict回傳409() {
            BusinessException ex = BusinessException.conflict("資料重複");

            assertThat(ex.getHttpStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("internal 回傳 500")
        void internal回傳500() {
            BusinessException ex = BusinessException.internal("系統錯誤");

            assertThat(ex.getHttpStatus()).isEqualTo(500);
        }
    }

    @Test
    @DisplayName("BusinessException 是 RuntimeException 子類，可被 catch(RuntimeException) 捕獲")
    void 是RuntimeException子類() {
        assertThatThrownBy(() -> {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }).isInstanceOf(RuntimeException.class);
    }
}
