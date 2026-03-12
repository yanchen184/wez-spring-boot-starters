package com.company.common.log.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogProperties 預設值與設定行為驗證
 *
 * 確保未設定任何 YAML 配置時，所有預設值符合業務預期。
 */
@DisplayName("LogProperties 配置屬性")
class LogPropertiesTest {

    @Nested
    @DisplayName("預設值驗證 — 未設定任何 YAML 配置時")
    class DefaultValues {

        private final LogProperties props = new LogProperties();

        @Test
        @DisplayName("日誌模組預設為啟用狀態")
        void 日誌模組預設為啟用狀態() {
            assertThat(props.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("預設記錄請求參數")
        void 預設記錄請求參數() {
            assertThat(props.isLogRequest()).isTrue();
        }

        @Test
        @DisplayName("預設記錄回應內容")
        void 預設記錄回應內容() {
            assertThat(props.isLogResponse()).isTrue();
        }

        @Test
        @DisplayName("預設記錄執行時間")
        void 預設記錄執行時間() {
            assertThat(props.isLogDuration()).isTrue();
        }

        @Test
        @DisplayName("慢請求閾值預設 3000 毫秒")
        void 慢請求閾值預設三秒() {
            assertThat(props.getSlowThresholdMs()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("預設不記錄回應 Body")
        void 預設不記錄回應Body() {
            assertThat(props.isLogResponseBody()).isFalse();
        }

        @Test
        @DisplayName("回應內容最大記錄長度預設 1000")
        void 回應內容最大記錄長度預設一千() {
            assertThat(props.getMaxResponseLength()).isEqualTo(1000);
        }

        @Test
        @DisplayName("預設遮罩欄位包含常見敏感欄位")
        void 預設遮罩欄位包含常見敏感欄位() {
            assertThat(props.getMaskFields())
                    .contains("password", "pwd", "secret", "token",
                            "authorization", "creditCard", "cardNumber",
                            "cvv", "idNumber", "ssn");
        }

        @Test
        @DisplayName("預設排除 actuator 等系統路徑")
        void 預設排除系統路徑() {
            assertThat(props.getExcludePatterns())
                    .contains("/actuator/**", "/health", "/metrics", "/favicon.ico");
        }
    }

    @Nested
    @DisplayName("自訂配置 — 透過 setter 覆蓋預設值")
    class CustomValues {

        @Test
        @DisplayName("可自訂慢請求閾值")
        void 可自訂慢請求閾值() {
            LogProperties props = new LogProperties();
            props.setSlowThresholdMs(5000L);

            assertThat(props.getSlowThresholdMs()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("可停用日誌模組")
        void 可停用日誌模組() {
            LogProperties props = new LogProperties();
            props.setEnabled(false);

            assertThat(props.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("可自訂最大回應長度")
        void 可自訂最大回應長度() {
            LogProperties props = new LogProperties();
            props.setMaxResponseLength(2000);

            assertThat(props.getMaxResponseLength()).isEqualTo(2000);
        }
    }
}
