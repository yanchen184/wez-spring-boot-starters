# Common Report Test

報表模組整合測試 — 涵蓋 Entity、Service、引擎、Controller、AutoConfiguration、限流。

---

## 快速開始

```bash
# 完整測試（需要 Redis，透過 Testcontainers 或本地 Redis）
mvn test -pl common-report/common-report-test -am

# 跳過限流測試（不需要 Redis）
mvn test -pl common-report/common-report-test -am -Dtest='!Phase7*'
```

---

## 功能總覽

- **7 個 Phase 整合測試** — TDD 規格文件風格，由底層到上層逐步驗證
- **Entity / Repository** — CRUD、關聯、索引
- **Service** — 引擎派發、路徑驗證、狀態機、BLOB 管理
- **引擎** — EasyExcel 四種模式（資料寫入、範本填充、多 Sheet、Pivot Table）
- **Controller** — HTTP API 端點（同步/非同步、狀態查詢、下載）
- **AutoConfiguration** — Bean 自動註冊驗證
- **限流** — Redis 分散式限流、併發模擬

---

## 測試覆蓋

| Phase | 測試類別 | 覆蓋項目 | 數量 |
|-------|---------|---------|------|
| 1 | `Phase1_EntityRepositoryTest` | ReportItem、ReportLog、ReportLogBlob、Repository CRUD | 7 |
| 2 | `Phase2_ReportServiceTest` | ReportService 引擎派發、路徑驗證、格式支援 | 6 |
| 3 | `Phase3_ReportLogServiceTest` | 狀態機（PENDING→PROCESSING→COMPLETED/FAILED）、BLOB 管理 | 10 |
| 4 | `Phase4_EasyExcelEngineTest` | EasyExcel 資料寫入、範本填充、多 Sheet、Pivot Table | 7 |
| 5 | `Phase5_ReportControllerTest` | HTTP API 端點（同步/非同步產製、狀態查詢、下載） | 4 |
| 6 | `Phase6_AutoConfigurationTest` | Bean 自動註冊驗證 | 5 |
| 7 | `Phase7_ThrottleTest` | Redis 分散式限流、併發模擬 | - |

---

## 核心 API

### 測試環境

| 項目 | 值 |
|------|-----|
| 資料庫 | H2（in-memory） |
| Redis | Testcontainers / 本地 Redis |
| 引擎 | EasyExcel（主要測試對象） |
| 框架 | JUnit 5 + Spring Boot Test |

### 執行指定 Phase

```bash
# 只跑 Phase 4（EasyExcel 引擎）
mvn test -pl common-report/common-report-test -am -Dtest='Phase4*'

# 只跑 Phase 1-3（Entity + Service）
mvn test -pl common-report/common-report-test -am -Dtest='Phase1*,Phase2*,Phase3*'
```

---

## 配置

測試使用 H2 in-memory 資料庫，無需額外配置。Phase 7 限流測試需要 Redis（自動透過 Testcontainers 啟動，或連接本地 Redis）。

---

## 設計決策

| 要什麼 | 不要什麼 |
|--------|----------|
| Phase 分層測試，由底層到上層 | 混在一起的大測試類 |
| H2 in-memory，無外部依賴 | 需要真實 DB 才能跑 |
| Testcontainers 自動管理 Redis | 手動啟動 Redis |
| TDD 規格文件風格命名 | 不清楚測什麼的測試名 |

---

## 依賴關係

```
common-report-test
├── common-report-spring-boot-starter      ← 被測目標
├── common-report-engine-easyexcel         ← 主要測試引擎
├── common-jpa-spring-boot-starter
├── common-response-spring-boot-starter
├── spring-boot-starter-data-jpa
├── spring-boot-starter-webmvc
├── spring-boot-starter-validation
├── spring-boot-starter-data-redis
├── spring-boot-starter-test
├── spring-boot-testcontainers
└── h2                                     ← in-memory 測試資料庫
```

---

## 專案結構與技術規格

### 目錄樹

```
common-report-test/
└── src/test/java/com/company/common/report/test/
    ├── ReportTestApplication.java          # 測試 Spring Boot 啟動類
    ├── ReportTest.java                     # 共用測試基類
    ├── Phase1_EntityRepositoryTest.java    # Entity / Repository
    ├── Phase2_ReportServiceTest.java       # ReportService 引擎派發
    ├── Phase3_ReportLogServiceTest.java    # 狀態機 + 審計
    ├── Phase4_EasyExcelEngineTest.java     # EasyExcel 四種模式
    ├── Phase5_ReportControllerTest.java    # HTTP API
    ├── Phase6_AutoConfigurationTest.java   # Bean 註冊驗證
    └── Phase7_ThrottleTest.java            # Redis 限流
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| JUnit | 5 |
| 資料庫 | H2 in-memory |
| Redis | Testcontainers |
| 測試檔案數 | 9 個 Java 類別 |

---

## 版本

### 1.0.0

- 初始版本
- Phase 1-6：Entity、Service、引擎、Controller、AutoConfiguration 共 39 個測試
- Phase 7：Redis 分散式限流測試
