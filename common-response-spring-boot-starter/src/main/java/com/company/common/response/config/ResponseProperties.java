package com.company.common.response.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Response Starter 配置屬性
 */
@ConfigurationProperties(prefix = "common.response")
public class ResponseProperties {

    /**
     * 是否啟用（預設 true）
     */
    private boolean enabled = true;

    /**
     * 排除路徑（Ant 風格）— 這些路徑的異常不會被 GlobalExceptionHandler 攔截
     * <p>
     * 預設排除 /actuator/**
     * <p>
     * 範例：
     * <pre>
     * common.response.exclude-paths:
     *   - /actuator/**
     *   - /internal/**
     * </pre>
     */
    private List<String> excludePaths = new ArrayList<>(List.of("/actuator/**"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}
