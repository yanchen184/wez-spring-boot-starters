package com.company.common.response.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResponseProperties 配置屬性驗證
 *
 * 確保預設值正確，以及 excludePaths 的 AntPathMatcher 比對邏輯。
 */
@DisplayName("ResponseProperties 配置屬性")
class ResponsePropertiesTest {

    @Nested
    @DisplayName("預設值驗證")
    class DefaultValues {

        private final ResponseProperties props = new ResponseProperties();

        @Test
        @DisplayName("預設為啟用狀態")
        void 預設為啟用狀態() {
            assertThat(props.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("預設排除路徑包含 /actuator/**")
        void 預設排除actuator() {
            assertThat(props.getExcludePaths()).containsExactly("/actuator/**");
        }
    }

    @Nested
    @DisplayName("AntPathMatcher 排除路徑比對")
    class AntPathMatcherBehavior {

        private final AntPathMatcher pathMatcher = new AntPathMatcher();

        @Test
        @DisplayName("/actuator/** 匹配 /actuator/health")
        void actuator萬用字元匹配子路徑() {
            assertThat(pathMatcher.match("/actuator/**", "/actuator/health")).isTrue();
        }

        @Test
        @DisplayName("/actuator/** 匹配 /actuator/prometheus")
        void actuator匹配prometheus() {
            assertThat(pathMatcher.match("/actuator/**", "/actuator/prometheus")).isTrue();
        }

        @Test
        @DisplayName("/actuator/** 不匹配 /api/users")
        void actuator不匹配api() {
            assertThat(pathMatcher.match("/actuator/**", "/api/users")).isFalse();
        }

        @Test
        @DisplayName("/internal/** 匹配 /internal/sync/data")
        void internal匹配多層子路徑() {
            assertThat(pathMatcher.match("/internal/**", "/internal/sync/data")).isTrue();
        }

        @Test
        @DisplayName("精確路徑 /health 只匹配 /health")
        void 精確路徑匹配() {
            assertThat(pathMatcher.match("/health", "/health")).isTrue();
            assertThat(pathMatcher.match("/health", "/health/check")).isFalse();
        }
    }

    @Nested
    @DisplayName("自訂排除路徑")
    class CustomExcludePaths {

        @Test
        @DisplayName("可設定自訂排除路徑清單")
        void 可設定自訂排除路徑() {
            ResponseProperties props = new ResponseProperties();
            props.setExcludePaths(List.of("/actuator/**", "/internal/**", "/swagger-ui/**"));

            assertThat(props.getExcludePaths()).hasSize(3);
            assertThat(props.getExcludePaths()).contains("/swagger-ui/**");
        }

        @Test
        @DisplayName("設定空清單時不排除任何路徑")
        void 空清單不排除() {
            ResponseProperties props = new ResponseProperties();
            props.setExcludePaths(List.of());

            assertThat(props.getExcludePaths()).isEmpty();
        }
    }
}
