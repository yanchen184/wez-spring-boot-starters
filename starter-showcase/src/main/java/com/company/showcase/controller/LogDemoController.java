package com.company.showcase.controller;

import com.company.common.log.annotation.Loggable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Log 展示用 API
 *
 * 加入 common-log-starter 後，所有 @RestController 自動記錄：
 * - --> 請求 log（method, URI, body, user）
 * - <-- 回應 log（status, 耗時, [SLOW] 標記）
 * - traceId/spanId 自動注入
 * - 敏感欄位自動遮罩
 */
@RestController
@RequestMapping("/api/log-demo")
public class LogDemoController {

    private static final Logger log = LoggerFactory.getLogger(LogDemoController.class);

    /**
     * 正常請求
     * GET /api/log-demo/ok
     */
    @GetMapping("/ok")
    public ResponseEntity<Map<String, Object>> ok() {
        log.info("這是一般的業務 log");
        return ResponseEntity.ok(Map.of(
                "message", "一切正常",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 慢請求 — 觸發 [SLOW] 標記
     * GET /api/log-demo/slow?delay=5
     */
    @GetMapping("/slow")
    public ResponseEntity<Map<String, Object>> slow(
            @RequestParam(defaultValue = "5") int delay) throws InterruptedException {
        int actualDelay = Math.max(1, Math.min(delay, 30));
        long delayMs = actualDelay * 1000L;
        log.info("模擬慢查詢開始... 預計延遲 {} 秒", actualDelay);
        Thread.sleep(delayMs);
        log.info("模擬慢查詢結束");
        return ResponseEntity.ok(Map.of(
                "message", "這個請求花了 " + actualDelay + " 秒",
                "cost", "~" + delayMs + "ms",
                "requestedDelay", delay,
                "actualDelay", actualDelay
        ));
    }

    /**
     * 錯誤請求 — 觸發 NullPointerException
     * GET /api/log-demo/error
     */
    @GetMapping("/error")
    public ResponseEntity<String> error() {
        log.info("即將發生錯誤...");
        String value = null;
        // 故意觸發 NullPointerException
        return ResponseEntity.ok(value.toUpperCase());
    }

    /**
     * POST 請求 — 展示 request body log
     * POST /api/log-demo/submit  body: {"name":"test","age":25}
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        log.info("收到提交資料，欄位數: {}", body.size());
        return ResponseEntity.ok(Map.of(
                "received", body,
                "fieldCount", body.size()
        ));
    }

    /**
     * 敏感資料 — 展示密碼遮罩
     * POST /api/log-demo/login-mock  body: {"username":"admin","password":"123456"}
     */
    @PostMapping("/login-mock")
    @Loggable(maskFields = {"password", "secret", "token"})
    public ResponseEntity<Map<String, String>> loginMock(@RequestBody Map<String, String> credentials) {
        log.info("模擬登入: user={}", credentials.get("username"));
        return ResponseEntity.ok(Map.of(
                "username", credentials.getOrDefault("username", "unknown"),
                "status", "logged_in",
                "token", "eyJhbGciOiJIUzI1NiJ9.mock-token-here"
        ));
    }

    /**
     * 不印 request body — @Loggable(logRequestBody = false)
     * POST /api/log-demo/no-request-log  body: {"data":"something"}
     */
    @PostMapping("/no-request-log")
    @Loggable(logRequestBody = false)
    public ResponseEntity<Map<String, Object>> noRequestLog(@RequestBody Map<String, Object> body) {
        log.info("這個 API 的 --> log 不帶 body");
        return ResponseEntity.ok(Map.of(
                "received", body,
                "note", "觀察 console：--> log 不帶 body={...}"
        ));
    }

    /**
     * 印出 response body — @Loggable(logResponseBody = true)
     * GET /api/log-demo/with-response-body
     */
    @GetMapping("/with-response-body")
    @Loggable(logResponseBody = true)
    public ResponseEntity<Map<String, Object>> withResponseBody() {
        return ResponseEntity.ok(Map.of(
                "userId", 1,
                "username", "admin",
                "token", "should-be-masked",
                "message", "觀察 console：<-- log 會帶 body={...}"
        ));
    }

    /**
     * 多層 log — 展示 traceId 串聯（4 筆 log 共用同一個 traceId）
     * GET /api/log-demo/trace-chain
     */
    @GetMapping("/trace-chain")
    public ResponseEntity<Map<String, Object>> traceChain() {
        log.info("[Step 1] 驗證使用者權限");
        log.info("[Step 2] 查詢資料庫");
        log.info("[Step 3] 組裝回應資料");
        log.info("[Step 4] 完成處理");
        return ResponseEntity.ok(Map.of(
                "message", "觀察 console，這 4 筆 log 共用同一個 traceId",
                "steps", 4
        ));
    }
}
