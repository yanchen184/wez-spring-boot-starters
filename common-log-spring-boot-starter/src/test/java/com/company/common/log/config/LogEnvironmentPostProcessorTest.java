package com.company.common.log.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogEnvironmentPostProcessor 行為驗證
 *
 * 確保在 Spring Boot 啟動早期正確注入 console log pattern，
 * 且不會覆蓋使用方已設定的 pattern。
 */
@DisplayName("LogEnvironmentPostProcessor 環境後處理器")
class LogEnvironmentPostProcessorTest {

    private final LogEnvironmentPostProcessor processor = new LogEnvironmentPostProcessor();

    @Test
    @DisplayName("未設定 logging.pattern.console 時，自動注入預設 pattern")
    void 未設定pattern時自動注入預設pattern() {
        MockEnvironment env = new MockEnvironment();

        processor.postProcessEnvironment(env, null);

        String pattern = env.getProperty("logging.pattern.console");
        assertThat(pattern).isNotNull();
        // 驗證 pattern 包含關鍵元素：traceId, spanId, 時間格式, 日誌等級
        assertThat(pattern)
                .contains("traceId")
                .contains("spanId")
                .contains("HH:mm:ss.SSS")
                .contains("%-5level");
    }

    @Test
    @DisplayName("使用方已設定 logging.pattern.console 時，不覆蓋")
    void 使用方已設定pattern時不覆蓋() {
        MockEnvironment env = new MockEnvironment();
        String customPattern = "%d{yyyy-MM-dd} %msg%n";
        env.setProperty("logging.pattern.console", customPattern);

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("logging.pattern.console")).isEqualTo(customPattern);
    }

    @Test
    @DisplayName("優先序設定為最低，確保其他 PostProcessor 可覆蓋")
    void 優先序設定為最低() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    @DisplayName("注入的 PropertySource 名稱為 commonLogDefaults")
    void 注入的PropertySource名稱正確() {
        MockEnvironment env = new MockEnvironment();

        processor.postProcessEnvironment(env, null);

        assertThat(env.getPropertySources().contains("commonLogDefaults")).isTrue();
    }

    @Test
    @DisplayName("注入的 PropertySource 位於最後，可被其他來源覆蓋")
    void 注入的PropertySource位於最後可被覆蓋() {
        MockEnvironment env = new MockEnvironment();
        // 先加一個自訂 source
        env.getPropertySources().addFirst(
                new MapPropertySource("custom", Map.of("some.key", "value")));

        processor.postProcessEnvironment(env, null);

        // commonLogDefaults 應在 custom 之後（優先序更低）
        var sources = env.getPropertySources();
        assertThat(sources.precedenceOf(sources.get("commonLogDefaults")))
                .isGreaterThan(sources.precedenceOf(sources.get("custom")));
    }
}
