package com.company.showcase.controller;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;

/**
 * Actuator + Observability 展示用 API
 *
 * 展示：
 * - @Timed: 自動記錄方法耗時（duration + count）
 * - @Counted: 自動記錄呼叫次數
 * - @Observed: 同時產生 metric + trace span
 * - MeterRegistry: 手動操作自訂 metric
 *
 * 搭配 Actuator 端點查看結果：
 * - /actuator/metrics/{name}
 * - /actuator/prometheus
 */
@RestController
@RequestMapping("/api/observability-demo")
public class ObservabilityDemoController {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityDemoController.class);
    private static final Random random = new Random();

    private final MeterRegistry meterRegistry;
    private final Counter customCounter;

    public ObservabilityDemoController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.customCounter = Counter.builder("showcase.custom.counter")
                .description("手動建立的自訂計數器")
                .tag("source", "demo")
                .register(meterRegistry);
    }

    /**
     * @Timed — 自動記錄耗時
     *
     * 打完後去 /actuator/metrics/showcase.timed.method 查看：
     * - COUNT: 呼叫次數
     * - TOTAL_TIME: 總耗時
     * - MAX: 最大耗時
     * - P50, P95, P99: 百分位數
     */
    @Timed(value = "showcase.timed.method", description = "展示 @Timed 自動計時", percentiles = {0.5, 0.95, 0.99})
    @GetMapping("/timed")
    public ResponseEntity<Map<String, Object>> timedMethod() throws InterruptedException {
        int delay = 10 + random.nextInt(200);
        Thread.sleep(delay);
        log.info("@Timed method executed, delay={}ms", delay);
        return ResponseEntity.ok(Map.of(
                "annotation", "@Timed",
                "metric", "showcase.timed.method",
                "simulatedDelay", delay + "ms",
                "howToCheck", "GET /actuator/metrics/showcase.timed.method"
        ));
    }

    /**
     * @Counted — 自動記錄呼叫次數
     *
     * 打完後去 /actuator/metrics/showcase.counted.method 查看 COUNT
     */
    @Counted(value = "showcase.counted.method", description = "展示 @Counted 自動計數")
    @PostMapping("/counted")
    public ResponseEntity<Map<String, Object>> countedMethod(@RequestBody Map<String, Object> body) {
        log.info("@Counted method called with {} fields", body.size());
        return ResponseEntity.ok(Map.of(
                "annotation", "@Counted",
                "metric", "showcase.counted.method",
                "received", body,
                "howToCheck", "GET /actuator/metrics/showcase.counted.method"
        ));
    }

    /**
     * @Observed — 同時產生 metric + trace span
     *
     * 這是 Micrometer 2 的重點功能：一個註解，兩種輸出
     */
    @Observed(name = "showcase.observed.method", contextualName = "process-order", lowCardinalityKeyValues = {"type", "demo"})
    @GetMapping("/observed")
    public ResponseEntity<Map<String, Object>> observedMethod() throws InterruptedException {
        int delay = 20 + random.nextInt(100);
        Thread.sleep(delay);
        log.info("@Observed method executed — metric + trace span 同時產生");
        return ResponseEntity.ok(Map.of(
                "annotation", "@Observed",
                "metric", "showcase.observed.method",
                "trace", "自動產生 trace span（看 console 的 traceId）",
                "simulatedDelay", delay + "ms",
                "howToCheck", "GET /actuator/metrics/showcase.observed.method"
        ));
    }

    /**
     * 手動操作自訂 Counter
     *
     * 展示用程式碼建立和操作 metric
     */
    @PostMapping("/custom-counter")
    public ResponseEntity<Map<String, Object>> incrementCustomCounter(@RequestBody Map<String, Object> body) {
        double amount = body.containsKey("amount") ? ((Number) body.get("amount")).doubleValue() : 1.0;
        customCounter.increment(amount);
        log.info("Custom counter incremented by {}, total={}", amount, customCounter.count());
        return ResponseEntity.ok(Map.of(
                "action", "increment",
                "amount", amount,
                "total", customCounter.count(),
                "howToCheck", "GET /actuator/metrics/showcase.custom.counter"
        ));
    }

    /**
     * 模擬不同結果（成功/失敗）— 展示 metric 按 tag 分類
     *
     * 打完後看 /actuator/metrics/http.server.requests?tag=uri:/api/observability-demo/random-result
     * 可以看到按 status 分類的統計
     */
    @GetMapping("/random-result")
    @Timed(value = "showcase.random.result", description = "隨機成功或失敗")
    public ResponseEntity<Map<String, Object>> randomResult() throws InterruptedException {
        Thread.sleep(10 + random.nextInt(50));
        if (random.nextBoolean()) {
            return ResponseEntity.ok(Map.of("result", "success", "howToCheck", "GET /actuator/metrics/showcase.random.result"));
        } else {
            throw new RuntimeException("Random failure for demo");
        }
    }
}
