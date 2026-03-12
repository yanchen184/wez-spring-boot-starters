package com.company.common.log.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 在 Spring Boot 啟動早期注入預設的 console log pattern。
 *
 * <p>使用 {@link EnvironmentPostProcessor} 確保在 logging 系統初始化之前設定好 pattern。
 * 設定以最低優先序加入（{@code addLast}），因此：
 * <ul>
 *   <li>使用方在 application.yml 設定 {@code logging.pattern.console} 會自動覆蓋</li>
 *   <li>使用方放 logback-spring.xml 也不受影響（logback-spring.xml 優先於 pattern property）</li>
 * </ul>
 *
 * <p>Pattern 格式：
 * <pre>
 * HH:mm:ss.SSS LEVEL [traceId 前 8 碼,spanId 前 8 碼] logger : 訊息
 * </pre>
 */
public class LogEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "commonLogDefaults";

    private static final String CONSOLE_PATTERN =
            "%d{HH:mm:ss.SSS} %-5level [%.8X{traceId},%.8X{spanId}] %-40.40logger{39} : %msg%n";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 如果使用方已經設定了 logging.pattern.console，不做任何事
        if (environment.containsProperty("logging.pattern.console")) {
            return;
        }

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("logging.pattern.console", CONSOLE_PATTERN);

        // addLast = 最低優先序，使用方的任何設定都會覆蓋
        environment.getPropertySources()
                .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    /**
     * 設定較低的優先序（數字越大優先序越低），
     * 讓其他 EnvironmentPostProcessor 可以在之前執行
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
