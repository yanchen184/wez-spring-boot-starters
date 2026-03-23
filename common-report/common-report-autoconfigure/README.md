# Common Report Autoconfigure

報表自動配置模組 — AutoConfiguration、REST Controller、配置屬性。

---

## 快速開始

> 一般使用者不需要直接引用此模組，引入 `common-report-spring-boot-starter` 即可。

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-autoconfigure</artifactId>
</dependency>
```

---

## 功能總覽

- **ReportAutoConfiguration** — 自動註冊 ReportService、ReportLogService、ReportThrottleService、ReportController
- **ReportAsyncConfiguration** — `@EnableAsync` + ThreadPool 配置
- **ReportController** — REST API 端點（同步/非同步產製、狀態查詢、下載）
- **ReportProperties** — `common.report.*` 配置屬性綁定
- **DTO** — ReportGenerateRequest、ReportStatusResponse

---

## 核心 API

### ReportAutoConfiguration

條件化 Bean 註冊：

- `@ConditionalOnProperty(prefix = "common.report", name = "enabled", matchIfMissing = true)` — 預設啟用
- `@EntityScan` + `@EnableJpaRepositories` — 自動掃描 report entity/repository
- 自動偵測已載入的引擎並註冊

### ReportAsyncConfiguration

```java
// 自動配置的 ThreadPool：
// - corePoolSize: common.report.async.core-pool-size (預設 2)
// - maxPoolSize: common.report.async.max-pool-size (預設 5)
// - queueCapacity: common.report.async.queue-capacity (預設 100)
// - rejectedExecutionHandler: CallerRunsPolicy
// - waitForTasksToCompleteOnShutdown: true
```

### ReportController

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/reports/generate` | 同步產製（直接回傳檔案） |
| POST | `/api/reports/generate-async` | 非同步產製（回傳 UUID） |
| GET | `/api/reports/status/{uuid}` | 查詢產製狀態 |
| GET | `/api/reports/download/{uuid}` | 下載產製結果 |
| GET | `/api/reports/engines` | 列出可用引擎 |

---

## 配置

完整 `common.report.*` 配置：

```yaml
common:
  report:
    enabled: true                          # 總開關（預設 true）

    allowed-template-prefixes:             # 允許的範本路徑前綴
      - templates/                         # 預設 templates/

    storage:
      type: database                       # database | filesystem（預設 database）
      path: /tmp/reports                   # filesystem 時的儲存路徑

    async:
      enabled: true                        # 啟用非同步產製（預設 true）
      core-pool-size: 2                    # 核心執行緒數（預設 2）
      max-pool-size: 5                     # 最大執行緒數（預設 5）
      queue-capacity: 100                  # 佇列容量（預設 100）

    cleanup:
      enabled: false                       # 啟用定期清理舊記錄（預設 false）
      retention-days: 90                   # 保留天數，清理 COMPLETED + FAILED（預設 90）

    throttle:
      enabled: true                        # 啟用 Redis 分散式限流（預設 true）
      default-max-concurrent: 10           # 預設最大併發數（預設 10）
      key-ttl: PT10M                       # Redis key TTL（預設 10 分鐘）
```

---

## 設計決策

| 要什麼 | 不要什麼 |
|--------|----------|
| `@ConditionalOnClass` 自動偵測引擎 | 手動註冊引擎 Bean |
| `@ConditionalOnProperty` 總開關 | 無法關閉的自動配置 |
| CallerRunsPolicy 不丟棄任務 | DiscardPolicy 或 AbortPolicy |
| 優雅關閉等待 60 秒 | 強制中斷正在產製的報表 |
| Entity/Repository 自動掃描 | 要求使用方手動加 @EntityScan |

---

## 依賴關係

```
common-report-autoconfigure
├── common-report-core                      ← 核心 SPI + Service
├── spring-boot-autoconfigure               ← AutoConfiguration 支援
├── spring-boot-starter-webmvc              ← REST API（provided）
├── spring-boot-starter-validation          ← @Valid（provided）
├── spring-boot-starter-data-jpa            ← @EntityScan（provided）
├── spring-boot-starter-data-redis          ← 限流（provided）
├── springdoc-openapi-starter-webmvc-ui     ← Swagger UI（provided）
├── common-jpa-spring-boot-starter          ← Entity 基類（provided）
└── common-response-spring-boot-starter     ← ApiResponse（provided）
```

---

## 專案結構與技術規格

### 目錄樹

```
common-report-autoconfigure/
└── src/main/java/com/company/common/report/
    ├── autoconfigure/
    │   ├── ReportAutoConfiguration.java
    │   ├── ReportAsyncConfiguration.java
    │   └── ReportProperties.java
    ├── controller/
    │   └── ReportController.java
    └── dto/
        ├── ReportGenerateRequest.java
        └── ReportStatusResponse.java
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 檔案數 | 6 個 Java 類別 |

---

## 版本

### 1.0.0

- 初始版本
- ReportAutoConfiguration：條件化 Bean 註冊、Entity/Repository 自動掃描
- ReportAsyncConfiguration：@EnableAsync + ThreadPool
- ReportController：5 個 REST API 端點
- ReportProperties：完整配置屬性綁定
