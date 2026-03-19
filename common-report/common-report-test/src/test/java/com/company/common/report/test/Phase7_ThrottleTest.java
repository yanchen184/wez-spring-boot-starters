package com.company.common.report.test;

import com.company.common.report.service.ReportThrottleService;
import com.company.common.response.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 7: Report Throttle (Redis Distributed Rate Limiting)
 *
 * === TDD Guide for Engineers ===
 *
 * 7.1 Per-Report Throttle
 *     -> What: Each report type has its own concurrent limit
 *     -> Why: "As a system, I prevent 100 people exporting the same report simultaneously"
 *
 * 7.2 Global Throttle
 *     -> What: Total concurrent report generation across all types
 *     -> Why: "As a system, I protect server resources from overload"
 *
 * 7.3 Custom Limits
 *     -> What: Individual reports can have different limits
 *     -> Why: "As an admin, heavy reports get lower limits than light ones"
 *
 * 7.4 Concurrent Simulation
 *     -> What: Multiple threads hit the throttle simultaneously
 *     -> Why: "As a system, I correctly limit under real concurrent load"
 *
 * === Implementation Checklist ===
 * [x] ReportThrottleService — Redis INCR/DECR with TTL
 * [x] acquire() — increment counter, reject if over limit
 * [x] release() — decrement counter, prevent negative
 * [x] Per-report + global two-layer throttle
 * [x] Custom limits per reportName via config map
 */
@DisplayName("Phase 7: Report Throttle")
class Phase7_ThrottleTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;

    /** 模擬 Redis 計數器（用 AtomicLong 模擬 INCR/DECR） */
    private final Map<String, AtomicLong> fakeRedis = new java.util.concurrent.ConcurrentHashMap<>();

    @BeforeEach
    void setup() {
        fakeRedis.clear();
        redisTemplate = mockRedisTemplate();
    }

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> mockRedisTemplate() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);

        // mock increment: 模擬 Redis INCR
        when(valueOps.increment(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return fakeRedis.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        });

        // mock decrement: 模擬 Redis DECR
        when(valueOps.decrement(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            AtomicLong counter = fakeRedis.get(key);
            return counter != null ? counter.decrementAndGet() : -1L;
        });

        // mock expire: 不做事
        when(template.expire(anyString(), any(Duration.class))).thenReturn(true);

        return template;
    }

    // ========================================================================
    // 7.1 Per-Report Throttle
    // ========================================================================

    @Nested
    @DisplayName("7.1 Per-Report Throttle")
    class PerReportThrottle {

        @Test
        @DisplayName("allows requests within limit")
        void withinLimit_allowed() {
            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, true, false, 10, 3, Map.of());

            // 3 次都允許（上限 3）
            assertThatCode(() -> throttle.acquire("employee-list")).doesNotThrowAnyException();
            assertThatCode(() -> throttle.acquire("employee-list")).doesNotThrowAnyException();
            assertThatCode(() -> throttle.acquire("employee-list")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("blocks request exceeding limit")
        void exceedLimit_blocked() {
            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, true, false, 10, 2, Map.of());

            // 前 2 次允許
            throttle.acquire("salary-report");
            throttle.acquire("salary-report");

            // 第 3 次被擋
            assertThatThrownBy(() -> throttle.acquire("salary-report"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("salary-report")
                    .hasMessageContaining("上限");
        }

        @Test
        @DisplayName("release frees up a slot")
        void release_freesSlot() {
            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, true, false, 10, 2, Map.of());

            throttle.acquire("report-a");
            throttle.acquire("report-a");

            // 滿了
            assertThatThrownBy(() -> throttle.acquire("report-a"))
                    .isInstanceOf(BusinessException.class);

            // 釋放一個
            throttle.release("report-a");

            // 又可以了
            assertThatCode(() -> throttle.acquire("report-a")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("different reports have independent limits")
        void differentReports_independent() {
            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, true, false, 10, 1, Map.of());

            // report-a 佔滿
            throttle.acquire("report-a");
            assertThatThrownBy(() -> throttle.acquire("report-a"))
                    .isInstanceOf(BusinessException.class);

            // report-b 不受影響
            assertThatCode(() -> throttle.acquire("report-b")).doesNotThrowAnyException();
        }
    }

    // ========================================================================
    // 7.2 Global Throttle
    // ========================================================================

    @Nested
    @DisplayName("7.2 Global Throttle")
    class GlobalThrottle {

        @Test
        @DisplayName("blocks when global limit exceeded")
        void globalLimit_blocked() {
            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, true, true, 3, 10, Map.of());

            // 3 個不同報表各 1 個 = 全域 3 個
            throttle.acquire("report-a");
            throttle.acquire("report-b");
            throttle.acquire("report-c");

            // 第 4 個被全域限制擋下
            assertThatThrownBy(() -> throttle.acquire("report-d"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("全域");
        }

        @Test
        @DisplayName("global disabled — no global limit")
        void globalDisabled_noLimit() {
            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, true, false, 2, 10, Map.of());

            // 全域關閉，不會有全域限制
            for (int i = 0; i < 10; i++) {
                throttle.acquire("report-" + i);
            }
            // 10 個都通過，沒被全域擋
        }
    }

    // ========================================================================
    // 7.3 Custom Limits
    // ========================================================================

    @Nested
    @DisplayName("7.3 Custom Limits")
    class CustomLimits {

        @Test
        @DisplayName("custom limit overrides default")
        void customLimit_overridesDefault() {
            // default = 10，但 heavy-report 客製為 1
            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, true, false, 10, 10,
                    Map.of("heavy-report", 1));

            throttle.acquire("heavy-report");

            // 第 2 個就被擋（上限 1）
            assertThatThrownBy(() -> throttle.acquire("heavy-report"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("heavy-report");

            // 沒有客製的用 default = 10
            for (int i = 0; i < 10; i++) {
                throttle.acquire("normal-report");
            }
        }

        @Test
        @DisplayName("throttle disabled — all requests pass")
        void disabled_allPass() {
            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, false, false, 1, 1, Map.of());

            // 關閉限流，全部通過
            for (int i = 0; i < 100; i++) {
                assertThatCode(() -> throttle.acquire("any-report")).doesNotThrowAnyException();
            }
        }
    }

    // ========================================================================
    // 7.4 Concurrent Simulation
    // ========================================================================

    @Nested
    @DisplayName("7.4 Concurrent Simulation")
    class ConcurrentSimulation {

        @Test
        @DisplayName("concurrent requests correctly limited")
        void concurrent_correctlyLimited() throws Exception {
            int limit = 3;
            int totalRequests = 10;

            ReportThrottleService throttle = new ReportThrottleService(
                    redisTemplate, true, false, 10, limit, Map.of());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger rejectCount = new AtomicInteger(0);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(totalRequests);

            ExecutorService executor = Executors.newFixedThreadPool(totalRequests);

            for (int i = 0; i < totalRequests; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 全部同時開始
                        throttle.acquire("concurrent-test");
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        rejectCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 開始！
            doneLatch.await();
            executor.shutdown();

            // 最多 3 個成功，其餘被擋
            assertThat(successCount.get()).isEqualTo(limit);
            assertThat(rejectCount.get()).isEqualTo(totalRequests - limit);
            assertThat(successCount.get() + rejectCount.get()).isEqualTo(totalRequests);
        }
    }
}
