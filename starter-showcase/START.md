# Starter Showcase

體驗 `common-log-starter` + `common-response-starter` + `common-jpa-starter` + `Redis` + `Actuator + Observability` 的效果。

## 啟動

```bash
cd /Users/yanchen/workspace/my-projects/starter-showcase

# 關閉 log/response starter（預設）— JPA 始終啟用
mvn spring-boot:run

# 開啟所有 starter
# 把 application.yml 的 profiles.active 改為 with，然後重啟
mvn spring-boot:run
```

開啟瀏覽器：http://localhost:8080/demo.html

H2 Console：http://localhost:8080/h2-console（JDBC URL: `jdbc:h2:mem:showcase`）

## 切換方式

在 `application.yml` 中切換 profile：

```yaml
spring:
  profiles:
    active: without    # 關閉 log/response starter
    # active: with     # 開啟 log/response starter
```

## API 列表

### Log & Response Demo

| API | 方法 | 展示功能 |
|-----|------|---------|
| `/api/log-demo/ok` | GET | 基本 log 格式 |
| `/api/log-demo/slow?delay=4` | GET | [SLOW] 慢請求標記 |
| `/api/log-demo/error` | GET | 錯誤 log + 統一錯誤回應 |
| `/api/log-demo/submit` | POST | request body log |
| `/api/log-demo/login-mock` | POST | 密碼遮罩（password → ***） |
| `/api/log-demo/no-request-log` | POST | // @Loggable(logRequest = false) |
| `/api/log-demo/with-response-body` | GET | // @Loggable(logResponseBody = true) |
| `/api/log-demo/trace-chain` | GET | traceId 串聯 |

### JPA Demo

| API | 方法 | 展示功能 |
|-----|------|---------|
| `/api/jpa-demo/products` | POST | 審計欄位自動填入（createdBy, createdDate） |
| `/api/jpa-demo/products` | GET | 查詢所有商品（觀察審計欄位） |
| `/api/jpa-demo/products/{id}` | PUT | lastModifiedBy / lastModifiedDate 自動更新 |
| `/api/jpa-demo/orders` | POST | 建立訂單（BaseEntity: 含 deleted + version） |
| `/api/jpa-demo/orders` | GET | findAllActive() — 只顯示未刪除 |
| `/api/jpa-demo/orders/all` | GET | findAll() — 含已刪除，對比差異 |
| `/api/jpa-demo/orders/{id}` | DELETE | softDeleteById() — 軟刪除 |
| `/api/jpa-demo/orders/{id}/restore` | PUT | restoreById() — 恢復刪除 |
| `/api/jpa-demo/orders/count` | GET | countActive() vs count() |

## 前後差異

### profile: without（關閉 log/response starter）
- console 只有手寫的 `log.info()`
- 沒有 `-->` / `<--` 自動 log
- 沒有 traceId/spanId
- 錯誤回應是 Spring 預設 Whitelabel JSON
- 密碼明文印出

### profile: with（開啟 log/response starter）
- 自動印出 `-->` 請求 log（含 method、URI、body、user）
- 自動印出 `<--` 回應 log（含 status、耗時）
- 慢請求自動升級 WARN + `[SLOW]` 標記
- 每筆 log 帶 traceId/spanId（前 8 碼）
- password / secret / token 自動遮罩為 `***`
- 錯誤回應自動包裝成 ApiResponse 格式
- // @Loggable 可控制個別 API 的 log 行為

### JPA Starter（始終啟用）
- AuditableEntity: 4 個審計欄位自動填入
- BaseEntity: 審計 + 軟刪除（deleted）+ 樂觀鎖（version）
- SoftDeleteRepository: findAllActive(), softDeleteById(), restoreById() 等
- DefaultAuditorAware: 從 SecurityContext 取使用者，無認證時為 "SYSTEM"

### Redis Demo

| API | 方法 | 展示功能 |
|-----|------|---------|
| `/api/redis-demo/set` | POST | RedisTemplate SET（可選 TTL） |
| `/api/redis-demo/get/{key}` | GET | RedisTemplate GET（含 TTL 倒數） |
| `/api/redis-demo/delete/{key}` | DELETE | RedisTemplate DELETE |
| `/api/redis-demo/keys` | GET | 列出所有 Redis key |
| `/api/redis-demo/products/{id}` | GET | @Cacheable — 第一次打 DB，之後從 Redis |
| `/api/redis-demo/products/{id}/cache` | DELETE | @CacheEvict — 清除單筆快取 |
| `/api/redis-demo/products/cache` | DELETE | @CacheEvict(allEntries) — 清除全部 |

### Redis 體驗流程
1. 先用 JPA 建立商品（POST /api/jpa-demo/products）
2. 用快取查詢（GET /api/redis-demo/products/1）— console 有 SQL（cache miss）
3. 再查一次 — 沒有 SQL（cache hit），回應更快
4. 清除快取 → 再查 → 又有 SQL（cache miss）
5. 用 KEYS * 觀察 Redis 裡的 key（如 `products::1`）

注意：使用 Embedded Redis（port 6370），免裝外部 Redis

### Actuator + Observability Demo

| API | 方法 | 展示功能 |
|-----|------|---------|
| `/actuator/health` | GET | 應用健康狀態（DB、Redis、磁碟） |
| `/actuator/info` | GET | 應用資訊（名稱、版本、描述） |
| `/actuator/metrics` | GET | 所有可用 metric 名稱列表 |
| `/actuator/metrics/{name}` | GET | 查詢特定 metric（含 P50/P95/P99） |
| `/actuator/prometheus` | GET | Prometheus 格式輸出（可直接接 Grafana） |
| `/actuator/caches` | GET | 快取管理（列出所有 Cache） |
| `/api/observability-demo/timed` | GET | @Timed — 自動記錄方法耗時 |
| `/api/observability-demo/counted` | POST | @Counted — 自動記錄呼叫次數 |
| `/api/observability-demo/observed` | GET | @Observed — 同時產生 metric + trace span |
| `/api/observability-demo/custom-counter` | POST | MeterRegistry 手動建立自訂 Counter |
| `/api/observability-demo/random-result` | GET | 隨機成功/失敗 — 按 status 分類統計 |

### Observability 體驗流程
1. 打 @Timed API 5~10 次，查 `/actuator/metrics/showcase.timed.method` — 看 COUNT、MAX、P50/P95/P99
2. 打 @Observed API — 觀察 console 的 traceId（metric + trace 同時產生）
3. 打「隨機成功/失敗」多次，查 `/actuator/metrics/showcase.random.result` — 按 status 分類
4. 查 `/actuator/metrics/http.server.requests` — 所有 HTTP 請求的自動統計（零程式碼）
5. 查 `/actuator/prometheus` — Prometheus 格式輸出

### 三種註解差異

| 註解 | 用途 | 產出 |
|------|------|------|
| `@Timed` | 記錄耗時 | duration + count + percentiles |
| `@Counted` | 記錄次數 | count（只累加） |
| `@Observed` | 全方位觀測 | metric + trace span（Micrometer 2 推薦）|
