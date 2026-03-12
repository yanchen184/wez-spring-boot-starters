package com.company.common.log.config;

import com.company.common.log.filter.ApiLogFilter;
import com.company.common.log.filter.TracingFilter;
import com.company.common.log.interceptor.ApiLogInterceptor;
import tools.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 日誌模組自動配置
 *
 * <p>功能：
 * <ol>
 *   <li>自動註冊 {@link ApiLogFilter}（Servlet Filter — 記錄 --> / <-- log）</li>
 *   <li>自動註冊 {@link ApiLogInterceptor}（HandlerInterceptor — 讀取 @Loggable 設定）</li>
 *   <li>整合 Micrometer Tracing（traceId/spanId 由 Spring 官方管理）</li>
 * </ol>
 *
 * <p>架構：
 * <pre>
 * Request → TracingFilter (MDC traceId/spanId)
 *         → ApiLogFilter (記錄 --> / <-- log, 計時, 慢請求偵測)
 *             → ApiLogInterceptor (讀取 @Loggable 設定存入 request attribute)
 *             → Controller
 * </pre>
 *
 * <p>Tracing 說明：
 * <ul>
 *   <li>traceId/spanId 由 Micrometer Tracing + OpenTelemetry 自動產生</li>
 *   <li>自動注入到 MDC，logback pattern 用 %X{traceId} 即可</li>
 *   <li>自動傳播到 RestTemplate/WebClient 的 outgoing 請求</li>
 *   <li>支援 W3C Trace Context 標準 (traceparent header)</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "common.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LogProperties.class)
public class LogAutoConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(LogAutoConfiguration.class);

    private final ApiLogInterceptor apiLogInterceptor;

    public LogAutoConfiguration() {
        this.apiLogInterceptor = new ApiLogInterceptor();
    }

    /**
     * 註冊 ApiLogInterceptor — 攔截所有請求，讀取 @Loggable 設定
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("[Common-Log] Registering ApiLogInterceptor");
        registry.addInterceptor(apiLogInterceptor)
                .addPathPatterns("/**");
    }

    /**
     * 註冊 ApiLogFilter — 記錄 --> / <-- API log
     *
     * <p>Order 設定為 {@code Ordered.HIGHEST_PRECEDENCE + 10}，
     * 在 TracingFilter (HIGHEST_PRECEDENCE) 之後、Spring Security Filter 之前。
     */
    @Bean
    @ConditionalOnMissingBean(ApiLogFilter.class)
    public FilterRegistrationBean<ApiLogFilter> apiLogFilter(
            ObjectProvider<ObjectMapper> objectMapperProvider,
            LogProperties logProperties) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        log.info("[Common-Log] Registering ApiLogFilter (Servlet Filter)");
        log.info("[Common-Log] Slow API threshold: {}ms", logProperties.getSlowThresholdMs());

        ApiLogFilter filter = new ApiLogFilter(objectMapper, logProperties);
        FilterRegistrationBean<ApiLogFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * 註冊 TracingFilter — 確保每個 HTTP 請求的 MDC 都有 traceId/spanId
     *
     * <p>設定最高優先序，在所有其他 filter 之前執行。
     */
    @Bean
    public FilterRegistrationBean<TracingFilter> tracingFilter(ObjectProvider<Tracer> tracerProvider) {
        log.info("[Common-Log] Registering TracingFilter (MDC traceId/spanId)");
        FilterRegistrationBean<TracingFilter> registration =
                new FilterRegistrationBean<>(new TracingFilter(tracerProvider));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
