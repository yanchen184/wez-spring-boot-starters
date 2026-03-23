# Common Log Spring Boot Starter

統一日誌模組 — API 自動日誌、Micrometer Tracing 整合、敏感資料遮罩、慢 API 告警

---

## 目錄

- [加入後你的專案自動獲得](#加入後你的專案自動獲得)
- [快速開始](#快速開始)
- [功能總覽](#功能總覽)
- [核心 API](#核心-api)
- [配置](#配置)
- [設計決策](#設計決策)
- [依賴關係](#依賴關係)
- [專案結構與技術規格](#專案結構與技術規格)
- [版本](#版本)

---

## 加入後你的專案自動獲得

| 功能 | 加入前 | 加入後 |
|------|--------|--------|
| API 請求/回應 log | 需要每支 API 手寫 `log.info()` | 所有 `@RestController` 自動記錄 `-->` / `<--` |
| traceId / spanId | 沒有，無法串聯同一請求的多筆 log | 每筆 log 自動帶 `[traceId,spanId]`，同一請求共用 |
| 敏感資料保護 | 密碼、token 明文印出 | password、token 等欄位自動遮罩為 `***` |
| 慢 API 偵測 | 需要自己計時、自己判斷 | 超過閾值自動升級 WARN + `[SLOW]` 標記 |
| 錯誤定位 | 需要翻 stack trace 找行數 | 自動印出 `error="NullPointerException:..." (Controller.java:57)` |
| Request Body 記錄 | 需要手動印參數 | GET 自動印 query params，POST 自動印 `@RequestBody` |
| 跨服務追蹤 | 需要自己傳遞 traceId | Micrometer + OpenTelemetry 自動傳播 `traceparent` header |
| Log Pattern | 需要手動配置 | 自動設定含 traceId/spanId 的 console pattern |
| 配置 | — | 零配置，引入依賴即生效 |

### 可用的 API

引入 starter 後，你可以直接使用：

| API | 用途 | 範例 |
|-----|------|------|
| `@Loggable` | 自訂單一 API 的 log 行為 | `@Loggable(logRequest = false)` 不印請求 |
| `@Loggable(maskFields)` | 自訂遮罩欄位 | `@Loggable(maskFields = {"password", "idNumber"})` |
| `@Loggable(slowThresholdMs)` | 自訂慢 API 閾值 | `@Loggable(slowThresholdMs = 5000)` 5 秒才算慢 |
| `@Loggable(logResponseBody)` | 印出回應 Body | `@Loggable(logResponseBody = true)` |
| `@Loggable(logRequest, logResponse, logDuration)` | 精細控制 | 只印耗時、不印請求等組合 |
| `common.log.*` 配置 | 全域設定 | `slow-threshold-ms`、`mask-fields`、`exclude-patterns` |

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-log-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 完成

不需要任何配置，所有 `@RestController` 自動記錄日誌。

---

## 功能總覽

- **API 自動日誌** — 所有 `@RestController` 自動記錄請求/回應/耗時
- **Request Body 記錄** — GET 印 query params，POST/PUT 印 `@RequestBody`
- **敏感遮罩** — password、token 等欄位自動遮罩為 `***`
- **Slow API 告警** — 超過閾值自動升級為 WARN 並標記 `[SLOW]`
- **Micrometer Tracing** — 自動產生 traceId/spanId，支援 W3C Trace Context 跨服務傳播
- **Response Body 記錄** — 可選印出回應 Body（遮罩敏感欄位、截斷過長內容）
- **零配置** — 引入依賴即自動生效

---

## 核心 API

### 日誌格式

```
HH:mm:ss.SSS LEVEL [traceId,spanId] logger : 訊息
```

#### Request（-->）
```
11:30:15.123 INFO  [d12e3e1f,08f395d1] ApiLogAspect : --> POST /api/auth/login body={username:admin, password:***} user=anonymous
11:30:15.456 INFO  [a1b2c3d4,e5f6a7b8] ApiLogAspect : --> GET /api/users user=admin
```

#### Response（<--）
```
11:30:15.789 INFO  [d12e3e1f,08f395d1] ApiLogAspect : <-- 200 POST /api/auth/login 274ms
11:30:20.456 WARN  [a1b2c3d4,e5f6a7b8] ApiLogAspect : <-- 200 GET /api/reports/export 5003ms [SLOW]
```

#### Response with Body（<-- body=）

開啟 `logResponseBody` 後，回應日誌會附帶遮罩後的 body：
```
11:30:15.789 INFO  [d12e3e1f,08f395d1] ApiLogAspect : <-- 200 POST /api/auth/login 274ms body={token:***, userId:1, username:admin}
11:30:20.456 WARN  [a1b2c3d4,e5f6a7b8] ApiLogAspect : <-- 200 GET /api/reports/export 5003ms [SLOW] body={total:1000, data:[...]}
```

#### Error（<-- 500）
```
11:30:15.999 ERROR [c7c0cdda,e36329fa] ApiLogAspect : <-- 500 GET /api/log-demo/error 3ms error="NullPointerException: Cannot invoke..." (LogDemoController.java:57)
```

### @Loggable 註解

```java
// 自訂遮罩欄位
@Loggable(maskFields = {"password", "token", "idNumber"})

// 自訂慢 API 閾值（此 API 5 秒才算慢）
@Loggable(slowThresholdMs = 5000)

// 不印 request body
@Loggable(logRequest = false)

// 只印耗時
@Loggable(logRequest = false, logResponse = false, logDuration = true)

// 印出回應 Body（遮罩敏感欄位）
@Loggable(logResponseBody = true)

// 印出回應 Body + 自訂遮罩欄位
@Loggable(logResponseBody = true, maskFields = {"password", "token", "idNumber"})
```

### Micrometer Tracing

使用 Spring 官方的 Micrometer Tracing（取代已廢棄的 Spring Cloud Sleuth）。

| 功能 | 說明 |
|------|------|
| traceId/spanId 產生 | Micrometer + OpenTelemetry 自動產生 |
| MDC 自動注入 | logback pattern 用 `%X{traceId}` 輸出 |
| 跨服務傳播 | RestTemplate/WebClient 自動帶 `traceparent` header |
| W3C Trace Context | 標準協定，跨語言相容 |

透過 `ObjectProvider<Tracer>` 注入，無 Tracer 環境也能正常運作。

#### 跨系統 traceId 傳播

Spring Boot 呼叫其他服務時，Micrometer 自動在 header 帶上：

```
traceparent: 00-{traceId}-{spanId}-{flags}
```

對方系統取得 traceId：

```java
// 解析 traceparent header
String traceparent = request.getHeader("traceparent");
String traceId = traceparent.split("-")[1];
MDC.put("traceId", traceId);
```

### 兩個 Pointcut 的分工

```java
// 1. 沒有 @Loggable 的 RestController 方法 → 用預設值
@Around("@within(RestController) && !@annotation(Loggable)")

// 2. 有 @Loggable 的方法 → 用註解上的自訂值
@Around("@annotation(loggable)")
```

### 敏感遮罩原理

```
任意物件 → Jackson convertValue → Map<String, Object> → 遍歷 key 遮罩 → 簡潔格式輸出
```

支援 Map、POJO、List，不需要知道具體型別。

---

## 配置

### application.yml

```yaml
common:
  log:
    enabled: true                # 是否啟用（預設 true）
    slow-threshold-ms: 3000      # 慢 API 閾值，超過則 WARN（預設 3000ms）
    mask-fields:                 # 需要遮罩的欄位（預設 password, token, creditCard）
      - password
      - token
      - creditCard
    log-response-body: false     # 是否印出回應 Body（預設 false）
    max-response-length: 1000    # 回應 Body 最大記錄長度（預設 1000）
    exclude-patterns:            # 排除的 URL，不記錄 log（預設如下）
      - /actuator/**
      - /health
      - /favicon.ico
```

> Log Pattern（含 traceId/spanId）由 starter 自動設定，無需手動配置。如需覆蓋，自行設定 `logging.pattern.console` 即可。

---

## 設計決策

### 要什麼

| 欄位 | 範例 | 原因 |
|------|------|------|
| HTTP Method + URI | `POST /api/auth/login` | 最核心，一眼知道哪支 API |
| Request Body | `body={username:admin, password:***}` | 排查問題需要知道傳了什麼 |
| User | `user=admin` | 知道誰觸發的請求 |
| Status Code | `200` / `500` | 快速判斷成功或失敗 |
| 耗時 | `274ms` | 效能監控 |
| traceId / spanId | `[d12e3e1f,08f395d1]`（前 8 碼） | 跨服務追蹤 |
| [SLOW] 標記 | 超過閾值自動 WARN | 慢 API 主動告警 |
| Error 行數 | `(LogDemoController.java:57)` | 錯誤時直接定位程式碼 |
| 敏感遮罩 | `password:***` | 避免機敏資料洩漏 |

### 不要什麼

| 欄位 | 原因 |
|------|------|
| IP 位址 | 本地都是 `0:0:0:0:0:0:0:1`，生產有 gateway 記錄 |
| Source Location（正常請求） | 看 URI 就知道哪支 API，只有 error 才需要精確行數 |
| 完整 traceId（32 碼） | 太長，前 8 碼足以識別 |
| Response Body（預設） | 可能很大且含敏感資料，預設不印（可透過 `logResponseBody` 開啟） |
| 訊息內重複印 traceId | MDC 的 `[traceId,spanId]` 已在 log pattern 中 |
| Body 截斷 | request body 通常不大，截斷反而丟失排查資訊 |

### 設計原則

1. **一行一件事** — `-->` 請求、`<--` 回應，方便 grep
2. **正常極簡，錯誤詳細** — error 才加 exception 類型、訊息、行數
3. **人類可讀優先** — `-->` / `<--` 箭頭、`{key:value}` 簡潔格式
4. **安全第一** — 預設遮罩常見敏感欄位，response body 預設不印

---

## 依賴關係

```
common-log-spring-boot-starter
├── spring-boot-starter (provided)
├── spring-boot-starter-web (provided)
├── spring-boot-starter-aop (provided)
├── micrometer-tracing-bridge-otel (optional)
├── opentelemetry-exporter-zipkin (optional)
└── jackson-databind (provided)
```

---

## 專案結構與技術規格

### 目錄樹

```
common-log-spring-boot-starter/
├── src/main/java/com/company/common/log/
│   ├── annotation/
│   │   └── Loggable.java                    # @Loggable 註解
│   ├── aspect/
│   │   └── ApiLogAspect.java                # AOP 切面（--> / <--）
│   ├── config/
│   │   ├── LogAutoConfiguration.java        # 自動配置（註冊 Aspect + Filter）
│   │   ├── LogProperties.java               # 配置屬性（common.log.*）
│   │   └── LogEnvironmentPostProcessor.java # 自動設定 console log pattern
│   └── filter/
│       └── TracingFilter.java               # MDC traceId/spanId 注入
├── src/main/resources/META-INF/
│   ├── spring.factories                     # EnvironmentPostProcessor SPI 註冊
│   ├── spring/
│   │   └── ...AutoConfiguration.imports     # AutoConfiguration SPI 註冊
│   └── spring-configuration-metadata.json   # IDE 自動提示 metadata
└── pom.xml
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 自動配置 | `AutoConfiguration.imports` + `spring.factories`（EPP） |
| Tracing | Micrometer Tracing + OpenTelemetry Bridge |
| AOP | `@Around` Pointcut（RestController + @Loggable） |
| Log Pattern | `EnvironmentPostProcessor`（logging 初始化前設定） |
| 遮罩 | Jackson `convertValue` → Map 遍歷 |

### 核心技術說明

#### 為什麼用 AOP 而不是 Filter / Interceptor？

| 方案 | 能拿到 `@RequestBody`？ | 能拿到 `@Loggable` 註解？ | 能拿到回傳值？ |
|------|------------------------|--------------------------|--------------|
| Servlet Filter | 需要包裝 InputStream | 不知道目標方法 | 不行 |
| HandlerInterceptor | 只有 HttpServletRequest | 要自己反射找 | 只有 ModelAndView |
| **AOP @Around** | 直接從方法參數取 | `@annotation(loggable)` | `joinPoint.proceed()` 回傳值 |

#### EnvironmentPostProcessor vs @Configuration

Log pattern 必須在 **logging 系統初始化之前** 設定好，一般的 `@Bean` 太晚了。

```
啟動順序：EnvironmentPostProcessor → logging 初始化 → @Configuration Bean 建立
                ↑ 我們在這裡設 pattern
```

- 用 `addLast`（最低優先序）加入，使用方在 `application.yml` 設的值自動覆蓋
- Spring Boot 4.x 中 EPP 透過 `spring.factories` 註冊（`.imports` 只用於 AutoConfiguration）

#### Micrometer Tracing 流程

```
HTTP Request 進來
    ↓
TracingFilter（最高優先序）
    ↓ Micrometer 自動產生 traceId/spanId → 寫入 MDC
    ↓
ApiLogAspect
    ↓ log.info("--> ...") → logback 從 MDC 取 %X{traceId} 自動印出
    ↓
Controller 業務邏輯
    ↓ log.info("業務 log") → 同一個 traceId（因為同一個 thread）
    ↓
ApiLogAspect
    ↓ log.info("<-- ...")
    ↓
TracingFilter finally
    ↓ 清理 MDC
```

跨服務呼叫時，Micrometer 自動在 RestTemplate / WebClient 帶上 `traceparent` header：
```
traceparent: 00-{traceId}-{spanId}-{flags}
```

---

## 版本

### 1.0.0

- Micrometer Tracing 整合
- API Log Aspect（`-->` / `<--` 箭頭格式）
- Request Body 記錄 + 敏感遮罩
- Response Body Logging（預設關閉，可選開啟）
- Slow API 告警（[SLOW] 標記）
- TracingFilter（MDC traceId/spanId 注入）
- @Loggable 註解

---

## TODO / Roadmap

- [ ] **JSON Log 格式** — 給 OpenSearch / ELK 用的結構化日誌輸出
