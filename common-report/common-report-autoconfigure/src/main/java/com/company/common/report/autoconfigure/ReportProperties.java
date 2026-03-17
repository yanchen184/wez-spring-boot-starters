package com.company.common.report.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 報表模組配置屬性
 */
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
        private int corePoolSize = 2;

        /** 最大執行緒數 */
        private int maxPoolSize = 5;

        /** 佇列容量 */
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
}
