package com.company.common.report.autoconfigure;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 報表模組配置屬性
 */
@Validated
@ConfigurationProperties(prefix = "common.report")
public class ReportProperties {

    /** 是否啟用報表模組 */
    private boolean enabled = true;

    /** 儲存設定 */
    private Storage storage = new Storage();

    /** 非同步設定 */
    private Async async = new Async();

    /** 清理設定 */
    private Cleanup cleanup = new Cleanup();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Async getAsync() {
        return async;
    }

    public void setAsync(Async async) {
        this.async = async;
    }

    public Cleanup getCleanup() {
        return cleanup;
    }

    public void setCleanup(Cleanup cleanup) {
        this.cleanup = cleanup;
    }

    /** 限流設定 */
    private Throttle throttle = new Throttle();

    public Throttle getThrottle() { return throttle; }
    public void setThrottle(Throttle throttle) { this.throttle = throttle; }

    /**
     * 儲存方式設定
     */
    public static class Storage {

        /** 儲存類型：database | filesystem */
        private String type = "database";

        /** 檔案系統儲存路徑（僅 type=filesystem 時生效） */
        private String path = "/tmp/reports";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * 非同步執行設定
     */
    public static class Async {

        /** 是否啟用非同步產製 */
        private boolean enabled = true;

        /** 核心執行緒數 */
        @Min(1)
        private int corePoolSize = 2;

        /** 最大執行緒數 */
        @Min(1)
        private int maxPoolSize = 5;

        /** 佇列容量 */
        @Min(1)
        private int queueCapacity = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    /**
     * 報表記錄清理設定
     */
    public static class Cleanup {

        /** 是否啟用定期清理 */
        private boolean enabled = false;

        /** 保留天數 */
        @Min(1)
        private int retentionDays = 90;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }

    /**
     * 報表產製限流設定（Redis 分散式限流）
     */
    public static class Throttle {

        /** 是否啟用限流 */
        private boolean enabled = true;

        /** 全域限流開關（預設關） */
        private boolean globalEnabled = false;

        /** 全域（跨機器）同時產製上限（globalEnabled=true 才生效） */
        @Min(1)
        private int globalMaxConcurrent = 10;

        /** 每種報表預設同時產製上限 */
        @Min(1)
        private int defaultMaxConcurrent = 10;

        /** 個別報表客製化上限（key = reportName） */
        private java.util.Map<String, Integer> limits = new java.util.HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isGlobalEnabled() {
            return globalEnabled;
        }

        public void setGlobalEnabled(boolean globalEnabled) {
            this.globalEnabled = globalEnabled;
        }

        public int getGlobalMaxConcurrent() {
            return globalMaxConcurrent;
        }

        public void setGlobalMaxConcurrent(int globalMaxConcurrent) {
            this.globalMaxConcurrent = globalMaxConcurrent;
        }

        public int getDefaultMaxConcurrent() {
            return defaultMaxConcurrent;
        }

        public void setDefaultMaxConcurrent(int defaultMaxConcurrent) {
            this.defaultMaxConcurrent = defaultMaxConcurrent;
        }

        public java.util.Map<String, Integer> getLimits() {
            return limits;
        }

        public void setLimits(java.util.Map<String, Integer> limits) {
            this.limits = limits;
        }
    }
}
