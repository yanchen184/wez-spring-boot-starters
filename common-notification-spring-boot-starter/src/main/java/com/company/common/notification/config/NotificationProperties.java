package com.company.common.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "common.notification")
public class NotificationProperties {

    /** Enable/disable the notification starter. */
    private boolean enabled = true;

    /** Default channels to use when message doesn't specify. */
    private Set<String> defaultChannels = Set.of("EMAIL");

    /** Email sender address. */
    private String fromAddress = "noreply@company.com";

    /** Maximum retry attempts for failed notifications. */
    private int maxRetry = 3;

    /** Days to retain completed/failed notification records. 0 = never clean. */
    private int retentionDays = 90;

    /** Async executor configuration. */
    private Async async = new Async();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getDefaultChannels() {
        return defaultChannels;
    }

    public void setDefaultChannels(Set<String> defaultChannels) {
        this.defaultChannels = defaultChannels;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public Async getAsync() {
        return async;
    }

    public void setAsync(Async async) {
        this.async = async;
    }

    public static class Async {
        private int corePoolSize = 2;
        private int maxPoolSize = 5;
        private int queueCapacity = 100;

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
}
