package com.company.common.response.handler;

import com.company.common.response.code.CommonErrorCode;
import com.company.common.response.config.ResponseProperties;
import com.company.common.response.dto.ApiResponse;
import com.company.common.response.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GlobalExceptionHandler 全域異常處理驗證
 *
 * 使用 MockHttpServletRequest 模擬各種異常場景，
 * 確保回應格式與 HTTP 狀態碼符合業務約定。
 */
@DisplayName("GlobalExceptionHandler 全域異常處理")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        ResponseProperties properties = new ResponseProperties();
        handler = new GlobalExceptionHandler(properties);
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.setMethod("POST");
    }

    @Nested
    @DisplayName("BusinessException 處理")
    class BusinessExceptionHandling {

        @Test
        @DisplayName("BusinessException 回傳對應的 httpStatus 和 code")
        void 回傳對應httpStatus和code() throws Exception {
            BusinessException ex = BusinessException.notFound("找不到訂單");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getCode()).isEqualTo("A0300");
            assertThat(response.getBody().getMessage()).isEqualTo("找不到訂單");
        }

        @Test
        @DisplayName("自訂 code 的 BusinessException 回傳 400")
        void 自訂code回傳400() throws Exception {
            BusinessException ex = new BusinessException("CUSTOM_001", "自訂錯誤");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getCode()).isEqualTo("CUSTOM_001");
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException 處理")
    class IllegalArgumentHandling {

        @Test
        @DisplayName("IllegalArgumentException 回傳 400 BAD_REQUEST")
        void 回傳400() throws Exception {
            IllegalArgumentException ex = new IllegalArgumentException("id 不可為負數");

            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST.getCode());
            assertThat(response.getBody().getMessage()).isEqualTo("id 不可為負數");
        }
    }

    @Nested
    @DisplayName("未知異常處理")
    class UnknownExceptionHandling {

        @Test
        @DisplayName("未知異常回傳 500 INTERNAL_SERVER_ERROR，不暴露詳細訊息")
        void 回傳500不暴露詳細訊息() throws Exception {
            Exception ex = new RuntimeException("NullPointerException at line 42");

            ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getCode()).isEqualTo(CommonErrorCode.INTERNAL_ERROR.getCode());
            // 不暴露原始錯誤訊息，使用通用訊息
            assertThat(response.getBody().getMessage()).isEqualTo(CommonErrorCode.INTERNAL_ERROR.getMessage());
        }
    }

    @Nested
    @DisplayName("路徑排除 — actuator 等系統路徑不處理")
    class PathExclusion {

        @Test
        @DisplayName("actuator 路徑下的異常會 re-throw，不處理")
        void actuator路徑reThrow() {
            request.setRequestURI("/actuator/health");
            BusinessException ex = BusinessException.internal("should re-throw");

            assertThatThrownBy(() -> handler.handleBusinessException(ex, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("actuator 子路徑也被排除")
        void actuator子路徑也被排除() {
            request.setRequestURI("/actuator/prometheus");
            Exception ex = new RuntimeException("should re-throw");

            assertThatThrownBy(() -> handler.handleException(ex, request))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("非排除路徑正常處理")
        void 非排除路徑正常處理() throws Exception {
            request.setRequestURI("/api/users");
            BusinessException ex = BusinessException.badRequest("test");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("自訂排除路徑")
    class CustomExcludePaths {

        @Test
        @DisplayName("可新增自訂排除路徑")
        void 可新增自訂排除路徑() {
            ResponseProperties props = new ResponseProperties();
            props.setExcludePaths(List.of("/actuator/**", "/internal/**"));
            GlobalExceptionHandler customHandler = new GlobalExceptionHandler(props);

            request.setRequestURI("/internal/sync");
            BusinessException ex = BusinessException.internal("internal error");

            assertThatThrownBy(() -> customHandler.handleBusinessException(ex, request))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
