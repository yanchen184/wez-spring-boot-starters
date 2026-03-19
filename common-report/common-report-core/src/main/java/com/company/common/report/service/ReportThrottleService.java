package com.company.common.report.service;

import com.company.common.response.code.CommonErrorCode;
import com.company.common.response.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * 報表產製限流服務（Redis 分散式）。
 *
 * <p>使用 Redis INCR/DECR 實現跨機器的併發控制。
 * 每個計數器設 TTL 防止異常時未釋放導致永久鎖定。
 */
public class ReportThrottleService {

    private static final Logger log = LoggerFactory.getLogger(ReportThrottleService.class);
    private static final String GLOBAL_KEY = "report:throttle:global";
    private static final String NAME_KEY_PREFIX = "report:throttle:name:";
    private static final Duration KEY_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean enabled;
    private final boolean globalEnabled;
    private final int globalMaxConcurrent;
    private final int defaultMaxConcurrent;
    private final Map<String, Integer> limits;

    public ReportThrottleService(RedisTemplate<String, Object> redisTemplate,
                                  boolean enabled,
                                  boolean globalEnabled,
                                  int globalMaxConcurrent,
                                  int defaultMaxConcurrent,
                                  Map<String, Integer> limits) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.globalEnabled = globalEnabled;
        this.globalMaxConcurrent = globalMaxConcurrent;
        this.defaultMaxConcurrent = defaultMaxConcurrent;
        this.limits = limits;
    }

    /**
     * 嘗試取得產製許可。
     *
     * @param reportName 報表名稱
     * @throws BusinessException 超過限制時拋出（HTTP 429）
     */
    public void acquire(String reportName) {
        if (!enabled) {
            return;
        }

        // 1. 全域限流
        if (globalEnabled) {
            Long globalCount = redisTemplate.opsForValue().increment(GLOBAL_KEY);
            redisTemplate.expire(GLOBAL_KEY, KEY_TTL);
            if (globalCount != null && globalCount > globalMaxConcurrent) {
                redisTemplate.opsForValue().decrement(GLOBAL_KEY);
                log.warn("--> throttle BLOCKED | global limit reached: {}/{}", globalCount - 1, globalMaxConcurrent);
                throw new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS,
                        "報表全域同時產製數已達上限（" + globalMaxConcurrent + "），請稍後再試");
            }
        }

        // 2. Per-report 限流
        String nameKey = NAME_KEY_PREFIX + reportName;
        int maxConcurrent = limits.getOrDefault(reportName, defaultMaxConcurrent);
        Long nameCount = redisTemplate.opsForValue().increment(nameKey);
        redisTemplate.expire(nameKey, KEY_TTL);
        if (nameCount != null && nameCount > maxConcurrent) {
            redisTemplate.opsForValue().decrement(nameKey);
            // 如果全域也加了，要退回
            if (globalEnabled) {
                redisTemplate.opsForValue().decrement(GLOBAL_KEY);
            }
            log.warn("--> throttle BLOCKED | report '{}' limit reached: {}/{}", reportName, nameCount - 1, maxConcurrent);
            throw new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS,
                    "報表「" + reportName + "」同時產製數已達上限（" + maxConcurrent + "），請稍後再試");
        }

        log.debug("--> throttle ACQUIRED | report='{}', count={}/{}, global={}",
                reportName, nameCount, maxConcurrent,
                globalEnabled ? redisTemplate.opsForValue().get(GLOBAL_KEY) : "N/A");
    }

    /**
     * 釋放產製許可。
     *
     * @param reportName 報表名稱
     */
    public void release(String reportName) {
        if (!enabled) {
            return;
        }

        String nameKey = NAME_KEY_PREFIX + reportName;
        Long nameCount = redisTemplate.opsForValue().decrement(nameKey);
        // 防止計數器變成負數
        if (nameCount != null && nameCount < 0) {
            redisTemplate.opsForValue().set(nameKey, 0, KEY_TTL);
        }

        if (globalEnabled) {
            Long globalCount = redisTemplate.opsForValue().decrement(GLOBAL_KEY);
            if (globalCount != null && globalCount < 0) {
                redisTemplate.opsForValue().set(GLOBAL_KEY, 0, KEY_TTL);
            }
        }

        log.debug("<-- throttle RELEASED | report='{}'", reportName);
    }
}
