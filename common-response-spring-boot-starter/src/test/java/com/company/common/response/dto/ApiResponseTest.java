package com.company.common.response.dto;

import com.company.common.response.code.CommonErrorCode;
import com.company.common.response.code.UserErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiResponse 統一回應封裝驗證
 *
 * 驗證所有靜態工廠方法產生的回應格式符合業務約定。
 */
@DisplayName("ApiResponse 統一回應封裝")
class ApiResponseTest {

    @Nested
    @DisplayName("成功回應 — ok 系列方法")
    class SuccessResponse {

        @Test
        @DisplayName("ok() 無資料時 success=true, code=SUCCESS, data=null")
        void ok無資料() {
            ApiResponse<Void> response = ApiResponse.ok();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("SUCCESS");
            assertThat(response.getMessage()).isEqualTo("Success");
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("ok(data) 帶資料時，data 正確帶出")
        void ok帶資料() {
            ApiResponse<String> response = ApiResponse.ok("hello");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("SUCCESS");
            assertThat(response.getData()).isEqualTo("hello");
        }

        @Test
        @DisplayName("ok(message, data) 自訂訊息與資料")
        void ok自訂訊息與資料() {
            ApiResponse<Integer> response = ApiResponse.ok("查詢成功", 42);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("查詢成功");
            assertThat(response.getData()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("失敗回應 — error 系列方法")
    class ErrorResponse {

        @Test
        @DisplayName("error(message) 使用 BAD_REQUEST 錯誤碼")
        void error只帶訊息() {
            ApiResponse<Void> response = ApiResponse.error("參數不合法");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST.getCode());
            assertThat(response.getMessage()).isEqualTo("參數不合法");
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("error(ErrorCode) 使用 ErrorCode 的 code 和 message")
        void error帶ErrorCode() {
            ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.NOT_FOUND);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo("A0300");
            assertThat(response.getMessage()).isEqualTo("資源不存在");
        }

        @Test
        @DisplayName("error(ErrorCode, message) 可覆蓋 ErrorCode 的預設訊息")
        void error帶ErrorCode並覆蓋訊息() {
            ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.NOT_FOUND, "找不到該訂單");

            assertThat(response.getCode()).isEqualTo("A0300");
            assertThat(response.getMessage()).isEqualTo("找不到該訂單");
        }

        @Test
        @DisplayName("error(code, message) 可用自訂 code 字串")
        void error自訂code字串() {
            ApiResponse<Void> response = ApiResponse.error("CUSTOM_001", "自訂錯誤");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo("CUSTOM_001");
            assertThat(response.getMessage()).isEqualTo("自訂錯誤");
        }

        @Test
        @DisplayName("可使用業務模組的 ErrorCode（如 UserErrorCode）")
        void 可使用業務模組ErrorCode() {
            ApiResponse<Void> response = ApiResponse.error(UserErrorCode.USER_NOT_FOUND);

            assertThat(response.getCode()).isEqualTo("USER_B001");
            assertThat(response.getMessage()).isEqualTo("用戶不存在");
        }
    }

    @Nested
    @DisplayName("驗證錯誤回應 — validationError")
    class ValidationErrorResponse {

        @Test
        @DisplayName("validationError 帶出欄位錯誤清單")
        void validationError帶欄位錯誤() {
            List<FieldError> errors = List.of(
                    FieldError.of("email", "格式不正確", "bad-email"),
                    FieldError.of("name", "不可為空")
            );

            ApiResponse<Void> response = ApiResponse.validationError(errors);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR.getCode());
            assertThat(response.getErrors()).hasSize(2);
            assertThat(response.getErrors().get(0).getField()).isEqualTo("email");
            assertThat(response.getErrors().get(1).getField()).isEqualTo("name");
        }
    }
}
