# Common Report Core

報表核心模組 — SPI 介面、Entity、Service、限流，不含 Web 層。

---

## 快速開始

> 一般使用者不需要直接引用此模組，引入 `common-report-spring-boot-starter` 即可。

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-core</artifactId>
</dependency>
```

---

## 功能總覽

- **SPI 介面** — `ReportEngine`，各引擎實作此介面即可自動註冊
- **核心 Service** — `ReportService`（引擎派發）、`ReportLogService`（審計記錄）、`ReportAsyncService`（非同步產製）
- **限流 Service** — `ReportThrottleService`（Redis 分散式併發控制）
- **Entity** — `ReportLog`、`ReportLogBlob`、`ReportItem`
- **Enum** — `ReportEngineType`、`OutputFormat`、`ReportStatus`
- **Builder 模式** — `ReportContext`、`SheetData`、`PivotConfig`、`ImageSource`

---

## 核心 API

### ReportEngine（SPI 介面）

所有引擎必須實作此介面：

```java
public interface ReportEngine {
    long MAX_FILE_SIZE = 50L * 1024 * 1024;  // 50 MB

    ReportEngineType getType();
    ReportResult generate(ReportContext context);
    ReportResult generateMerged(List<ReportContext> contexts);
    boolean supports(OutputFormat format);
}
```

### ReportContext（Builder 模式）

報表產製的所有參數透過 Builder 傳入：

```java
ReportContext context = ReportContext.builder()
    .engineType(ReportEngineType.EASYEXCEL)   // 引擎類型
    .outputFormat(OutputFormat.XLSX)           // 輸出格式
    .templatePath("templates/employees.xlsx")  // 範本路徑（可選）
    .fileName("employees.xlsx")               // 輸出檔名
    .data(employeeList)                       // 資料寫入
    .headClass(EmployeeDTO.class)             // EasyExcel 欄位定義（可選）
    .sheets(List.of(sheet1, sheet2))          // 多 Sheet（可選）
    .images(Map.of("logo", imageSource))      // 圖片（可選）
    .pivots(List.of(pivotConfig))             // 樞紐分析表（可選）
    .parameters(Map.of("title", "報表"))       // 模板參數（可選）
    .build();
```

### ReportResult

產製結果，包含檔案內容和 metadata：

```java
ReportResult result = reportService.generate(context);
byte[] content = result.getContent();
String contentType = result.getContentType();
String fileName = result.getFileName();
```

### ReportService

引擎派發主入口：

```java
// 單一報表
ReportResult result = reportService.generate(context);

// 多報表合併
ReportResult merged = reportService.generateMerged(List.of(ctx1, ctx2));
```

### ReportLogService

審計記錄管理 + 狀態機：

```java
String uuid = logService.createLog("月報表", "monthly.xlsx");
logService.updateStatus(uuid, ReportStatus.PROCESSING);
logService.updateStatus(uuid, ReportStatus.COMPLETED, content);
ReportLog log = logService.findByUuid(uuid);
```

### ReportAsyncService

```java
asyncService.generateAsync(context, uuid);
```

### ReportThrottleService

Redis 分散式限流：

```java
throttleService.acquire("report-type-key");   // 取得許可，超過上限拋 BusinessException
throttleService.release("report-type-key");   // 釋放許可
```

---

## 配置

此模組為純 library，無自身配置。配置由 `common-report-autoconfigure` 模組的 `ReportProperties` 管理。

---

## Enum 一覽

| Enum | 值 |
|------|-----|
| `ReportEngineType` | `EASYEXCEL`, `JASPER`, `XDOCREPORT`, `EXPORT` |
| `OutputFormat` | `XLSX`, `XLS`, `CSV`, `PDF`, `DOCX`, `ODT`, `ODS`, `HTML`, `XML` |
| `ReportStatus` | `PENDING` → `PROCESSING` → `COMPLETED` / `FAILED` |

---

## Entity 說明

| Entity | Table | 說明 |
|--------|-------|------|
| `ReportItem` | REPORT_ITEM | 報表定義（名稱、範本路徑、引擎類型） |
| `ReportLog` | REPORT_LOG | 產製記錄（UUID、狀態、開始/結束時間、樂觀鎖） |
| `ReportLogBlob` | REPORT_LOG_BLOB | 產製檔案（VARBINARY(MAX)，與 ReportLog 分離避免載入大檔案） |

### 狀態機

```
PENDING → PROCESSING → COMPLETED
      ↘               ↘ FAILED

終態（COMPLETED / FAILED）不可再轉移。
```

---

## 設計決策

| 要什麼 | 不要什麼 |
|--------|----------|
| 純 library，不依賴 web | 綁死 Controller |
| SPI 介面，引擎可插拔 | 寫死的引擎判斷 |
| Builder 模式，參數清晰 | Map 傳參或超長建構子 |
| BLOB 分離表 | 全部塞同一張表 |
| 狀態機驗證 | 任意狀態跳轉 |
| @Version 樂觀鎖 | 無保護的併發更新 |

---

## 依賴關係

```
common-report-core
├── common-jpa-spring-boot-starter          ← Entity 基類（provided）
├── common-response-spring-boot-starter     ← BusinessException（provided）
├── spring-boot-starter-data-jpa            ← JPA（provided）
└── spring-boot-starter-data-redis          ← Redis 限流（provided）
```

---

## 專案結構與技術規格

### 目錄樹

```
common-report-core/
└── src/main/java/com/company/common/report/
    ├── entity/
    │   ├── ReportItem.java
    │   ├── ReportLog.java
    │   └── ReportLogBlob.java
    ├── enums/
    │   ├── OutputFormat.java
    │   ├── ReportEngineType.java
    │   └── ReportStatus.java
    ├── repository/
    │   ├── ReportItemRepository.java
    │   ├── ReportLogBlobRepository.java
    │   └── ReportLogRepository.java
    ├── service/
    │   ├── ReportAsyncService.java
    │   ├── ReportLogService.java
    │   ├── ReportService.java
    │   └── ReportThrottleService.java
    └── spi/
        ├── ImageSource.java
        ├── PivotConfig.java
        ├── ReportContext.java
        ├── ReportEngine.java
        ├── ReportResult.java
        └── SheetData.java
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 檔案數 | 19 個 Java 類別 |

---

## 版本

### 1.0.0

- 初始版本
- SPI 介面：ReportEngine、ReportContext、ReportResult、SheetData、PivotConfig、ImageSource
- Entity：ReportItem、ReportLog、ReportLogBlob + 狀態機
- Service：ReportService、ReportLogService、ReportAsyncService、ReportThrottleService
- Enum：ReportEngineType、OutputFormat、ReportStatus
