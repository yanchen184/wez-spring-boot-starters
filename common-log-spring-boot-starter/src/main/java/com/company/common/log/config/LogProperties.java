package com.company.common.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * 日誌模組配置屬性
 *
 * 使用方式（application.yml）：
 * <pre>
 * common:
 *   log:
 *     enabled: true
 *     log-request: true
 *     log-response: true
 *     mask-fields:
 *       - password
 *       - creditCard
 * </pre>
 *
 * @author Platform Team
 * @version 1.0.0
 */
@ConfigurationProperties(prefix = "common.log")
public class LogProperties {

    /**
     * 是否啟用日誌模組
     * 預設：true
     */
    private boolean enabled = true;

    /**
     * 是否記錄回應內容
     * 預設：true
     */
    private boolean logResponse = true;

    /**
     * 是否記錄執行時間
     * 預設：true
     */
    private boolean logDuration = true;

    /**
     * 需要遮罩的敏感欄位
     */
    private Set<String> maskFields = Set.of(
            "password", "pwd", "secret", "token",
            "authorization", "creditCard", "cardNumber",
            "cvv", "idNumber", "ssn"
    );

    /**
     * API 慢請求閾值（毫秒）
     * 超過此閾值的回應會以 WARN 等級記錄並標記 [SLOW]
     * 預設：3000ms
     */
    private long slowThresholdMs = 3000;

    /**
     * 是否記錄回應 Body（遮罩敏感欄位後印出）
     * 預設：false
     */
    private boolean logResponseBody = false;

    /**
     * 回應內容最大記錄長度
     * 預設：1000
     */
    private int maxResponseLength = 1000;

    /**
     * 排除的 URL Pattern（不記錄 log）
     * 例如：健康檢查、metrics
     */
    private Set<String> excludePatterns = Set.of(
            "/actuator/**",
            "/health",
            "/metrics",
            "/favicon.ico",
            "/ws/**"
    );

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogResponse() {
        return logResponse;
    }

    public void setLogResponse(boolean logResponse) {
        this.logResponse = logResponse;
    }

    public boolean isLogDuration() {
        return logDuration;
    }

    public void setLogDuration(boolean logDuration) {
        this.logDuration = logDuration;
    }

    public Set<String> getMaskFields() {
        return maskFields;
    }

    public void setMaskFields(Set<String> maskFields) {
        this.maskFields = maskFields;
    }

    public boolean isLogResponseBody() {
        return logResponseBody;
    }

    public void setLogResponseBody(boolean logResponseBody) {
        this.logResponseBody = logResponseBody;
    }

    public int getMaxResponseLength() {
        return maxResponseLength;
    }

    public void setMaxResponseLength(int maxResponseLength) {
        this.maxResponseLength = maxResponseLength;
    }

    public Set<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(Set<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public long getSlowThresholdMs() {
        return slowThresholdMs;
    }

    public void setSlowThresholdMs(long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }
}
