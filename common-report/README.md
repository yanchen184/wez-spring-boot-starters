# Common Report Spring Boot Starter

Spring Boot 4.0 報表產製 Starter，提供多引擎支援、非同步產製、審計記錄，加依賴即可使用。

## 目錄

1. [加入後你的專案自動獲得](#加入後你的專案自動獲得)
2. [快速開始](#快速開始)
3. [功能總覽](#功能總覽)
4. [核心 API](#核心-api)
5. [配置](#配置)
6. [REST API](#rest-api)
7. [EasyExcel 引擎](#easyexcel-引擎)
8. [xDocReport 引擎](#xdocreport-引擎)
9. [JasperReports 引擎](#jasperreports-引擎)
10. [SPI 自訂引擎](#spi-自訂引擎)
11. [非同步產製](#非同步產製)
12. [資料庫 Entity](#資料庫-entity)
13. [Pivot Table 記憶體優化](#pivot-table-記憶體優化)
14. [設計決策](#設計決策)
15. [依賴關係](#依賴關係)
16. [專案結構與技術規格](#專案結構與技術規格)
17. [版本](#版本)

---

## 加入後你的專案自動獲得

| 功能 | 說明 |
|------|------|
| 多引擎報表產製 | EasyExcel（XLSX/XLS/CSV）、xDocReport（DOCX/PDF/ODT）、JasperReports（PDF/XLSX），可插拔 |
| 非同步產製 | Spring @Async + ThreadPool，UUID 追蹤狀態與下載 |
| 審計記錄 | 每次產製自動記錄（UUID、狀態、開始/結束時間、檔案 BLOB） |
| REST API | 同步/非同步產製、狀態查詢、下載、引擎列表端點 |
| Redis 分散式限流 | `ReportThrottleService` 控制報表併發數 |
| 路徑遍歷保護 | templatePath 禁止 `..`、`\`、`://`、`%` |
| 檔案大小限制 | 50MB 上限，防止 OOM |
| 零配置 | 加依賴自動生效，所有屬性皆有合理預設值 |

---

## 快速開始

### 1. 加入依賴

```xml
<!-- Starter 核心 -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-spring-boot-starter</artifactId>
</dependency>

<!-- 選擇需要的引擎（至少一個） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-easyexcel</artifactId>
</dependency>

<!-- Word 套表（可選） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-xdocreport</artifactId>
</dependency>
```

### 2. 在程式碼中使用

```java
@RestController
public class MyReportController {

    private final ReportService reportService;

    // 匯出 Excel
    @GetMapping("/export-excel")
    public void exportExcel(HttpServletResponse response) throws Exception {
        ReportContext context = ReportContext.builder()
                .engineType(ReportEngineType.EASYEXCEL)
                .outputFormat(OutputFormat.XLSX)
                .fileName("report.xlsx")
                .data(dataList)
                .build();

        ReportResult result = reportService.generate(context);
        writeFile(response, result);
    }

    // 匯出 Word
    @GetMapping("/export-word")
    public void exportWord(HttpServletResponse response) throws Exception {
        ReportContext context = ReportContext.builder()
                .engineType(ReportEngineType.XDOCREPORT)
                .outputFormat(OutputFormat.DOCX)
                .templatePath("templates/contract.docx")
                .parameter("clientName", "王小明")
                .data(itemList)
                .fileName("合約書.docx")
                .build();

        ReportResult result = reportService.generate(context);
        writeFile(response, result);
    }

    private void writeFile(HttpServletResponse response, ReportResult result) throws Exception {
        response.setContentType(result.getContentType());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(result.getFileName(), UTF_8) + "\"");
        response.getOutputStream().write(result.getContent());
    }
}
```

### 3. 完成

引入依賴後，以下功能自動生效：

- `ReportService` — 報表產製（依引擎類型自動派發）
- `ReportLogService` — 審計記錄（自動記錄每次產製）
- `ReportAsyncService` — 非同步產製
- `ReportController` — REST API（產製 / 狀態查詢 / 下載）

---

## 功能總覽

- **SPI 引擎插拔** — `ReportEngine` 介面 + 策略模式，加依賴自動註冊，不改既有程式碼
- **Builder 模式** — `ReportContext.builder()` 流暢 API，參數多且可選
- **多引擎支援** — EasyExcel（Excel）、xDocReport（Word/PDF）、JasperReports（PDF/Excel）
- **非同步產製** — Spring @Async + ThreadPool，UUID 追蹤狀態
- **審計記錄** — ReportLog 自動記錄每次產製的時間、狀態、結果
- **狀態機** — PENDING → PROCESSING → COMPLETED/FAILED，終態不可再轉移
- **樂觀鎖** — `@Version` 防止併發修改 ReportLog
- **Redis 分散式限流** — 控制報表併發數，防止資源耗盡
- **路徑遍歷保護** — templatePath 驗證 `..`、`\`、`://`、`%`
- **檔案大小限制** — 50MB 上限保護，防 OOM
- **批次清理** — JPQL 批次刪除（先刪 blob 再刪 log），也清理 FAILED 記錄
- **Thread Pool 優雅關閉** — CallerRunsPolicy + waitForTasksToComplete
- **UUID 格式驗證** — 防止亂字串打 DB 的 DoS 攻擊
- **core 不依賴 web** — 可在 batch job 使用
- **Pivot Table 記憶體優化** — temp file + memory-mapped IO，降低 35% 記憶體

---

## 核心 API

### ReportService

報表產製的主要入口，依 `engineType` 自動派發到對應引擎：

```java
@Autowired
private ReportService reportService;

// 單一報表產製
ReportResult result = reportService.generate(context);

// 多報表合併（多 Sheet）
ReportResult merged = reportService.generateMerged(List.of(context1, context2));
```

### ReportContext（Builder 模式）

```java
ReportContext context = ReportContext.builder()
    .engineType(ReportEngineType.EASYEXCEL)   // 引擎類型
    .outputFormat(OutputFormat.XLSX)           // 輸出格式
    .templatePath("templates/report.xlsx")    // 範本路徑（可選）
    .fileName("report.xlsx")                  // 輸出檔名
    .data(dataList)                           // 資料 List
    .headClass(UserDTO.class)                 // EasyExcel 欄位定義（可選）
    .sheets(List.of(sheet1, sheet2))          // 多 Sheet（可選）
    .images(Map.of("logo", imageSource))      // 圖片（可選）
    .pivots(List.of(pivotConfig))             // 樞紐分析表（可選）
    .parameters(Map.of("title", "報表"))       // 模板參數（可選）
    .build();
```

### ReportLogService

```java
@Autowired
private ReportLogService logService;

// 建立記錄
String uuid = logService.createLog("月報表", "monthly.xlsx");

// 查詢狀態
ReportLog log = logService.findByUuid(uuid);

// 更新狀態
logService.updateStatus(uuid, ReportStatus.COMPLETED, content);
```

### ReportAsyncService

```java
@Autowired
private ReportAsyncService asyncService;

// 非同步產製（背景執行）
asyncService.generateAsync(context, uuid);
```

### ReportEngine（SPI 介面）

```java
public interface ReportEngine {
    long MAX_FILE_SIZE = 50L * 1024 * 1024;  // 50 MB

    ReportEngineType getType();
    ReportResult generate(ReportContext context);
    ReportResult generateMerged(List<ReportContext> contexts);
    boolean supports(OutputFormat format);
}
```

---

## 配置

所有配置前綴：`common.report`

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

    # 以下為待開發功能，配置已預留
    transfer:
      enabled: false                       # 啟用 JodConverter 轉檔（預設 false）
      libre-office-home: /usr/lib/libreoffice
```

---

## REST API

### 內建端點

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/reports/generate` | 同步產製（直接回傳檔案） |
| POST | `/api/reports/generate-async` | 非同步產製（回傳 UUID） |
| GET | `/api/reports/status/{uuid}` | 查詢非同步狀態 |
| GET | `/api/reports/download/{uuid}` | 下載已完成的報表 |
| GET | `/api/reports/engines` | 列出可用引擎 |

> 注意：此 Controller 不包含權限控制。使用方應透過 Spring Security 配置保護 `/api/reports/**`。

### 與 GlobalResponseAdvice 共存

如果同時使用 `common-response-spring-boot-starter`，檔案下載端點需要用 `void` + `HttpServletResponse` 寫出，避免被包裝成 `ApiResponse`：

```java
// 正確
@GetMapping("/download")
public void download(HttpServletResponse response) { ... }

// 錯誤 — ClassCastException
@GetMapping("/download")
public ResponseEntity<byte[]> download() { ... }
```

或在配置中排除路徑：

```yaml
common:
  response:
    exclude-paths:
      - /api/reports/download/**
      - /your/download/path/**
```

---

## EasyExcel 引擎

### 引擎總覽

| 格式 | 說明 |
|------|------|
| XLSX | Excel 2007+（含 Pivot Table 支援） |
| XLS | Excel 97-2003 |
| CSV | 逗號分隔值 |

### 資料寫入模式

直接傳入 `List<?>` 資料，EasyExcel 自動產生 Excel：

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.EASYEXCEL)
        .outputFormat(OutputFormat.XLSX)
        .fileName("users.xlsx")
        .data(userList)
        .build();
```

DTO 使用 EasyExcel 註解控制欄位名稱和寬度：

```java
public class UserExcelData {
    @ExcelProperty("姓名")
    @ColumnWidth(20)
    private String name;

    @ExcelProperty("Email")
    @ColumnWidth(30)
    private String email;
}
```

### 範本填充模式

提供 `.xlsx` 範本，用變數填充：

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.EASYEXCEL)
        .outputFormat(OutputFormat.XLSX)
        .templatePath("templates/monthly-report.xlsx")
        .parameter("title", "2026年3月報表")
        .parameter("date", LocalDate.now().toString())
        .data(detailList)
        .build();
```

### 多 Sheet 模式

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.EASYEXCEL)
        .outputFormat(OutputFormat.XLSX)
        .fileName("report.xlsx")
        .sheets(List.of(
            SheetData.of("員工", employeeList, EmployeeDTO.class),
            SheetData.of("部門", deptList, DeptDTO.class)
        ))
        .build();
```

### 樞紐分析表模式

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.EASYEXCEL)
        .outputFormat(OutputFormat.XLSX)
        .fileName("pivot.xlsx")
        .data(salesData)
        .headClass(SalesDTO.class)
        .pivots(List.of(
            PivotConfig.builder()
                .sourceSheet("Sheet1")
                .targetSheet("樞紐分析")
                .rowLabels(List.of("region"))
                .columnLabels(List.of("quarter"))
                .valueFields(List.of(PivotConfig.ValueField.sum("amount", "銷售額")))
                .build()
        ))
        .build();
```

### CustomMergeStrategy

相同值的儲存格自動垂直合併，適合分組報表：

```
| 部門   | 姓名   | 薪資   |
|--------|--------|--------|
| 研發部 | 王小明 | 65,000 |
|        | 李小華 | 58,000 |  ← 「研發部」自動合併
|        | 劉建宏 | 72,000 |
| 業務部 | 陳大文 | 70,000 |
|        | 林志偉 | 62,000 |  ← 「業務部」自動合併
```

---

## xDocReport 引擎

用 Word 範本（`.docx`）+ Velocity 變數替換，產出 Word 或 PDF。適合合約、公文、通知書。

### 支援格式

| OutputFormat | 說明 |
|-------------|------|
| `DOCX` | 直接輸出 Word |
| `PDF` | Word → PDF（需要 xdocreport converter） |
| `ODT` | OpenDocument Text |

### Word 範本

建立 `.docx` 範本，用 Velocity 語法寫變數：

```
合約書

甲方：$clientName
日期：$contractDate

明細：
#foreach($item in $items)
  - $item.name：$item.amount 元
#end
```

### 使用方式

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.XDOCREPORT)
        .outputFormat(OutputFormat.DOCX)   // 或 PDF
        .templatePath("templates/contract.docx")
        .parameter("clientName", "王小明")
        .parameter("contractDate", "2026-03-17")
        .data(itemList)                    // 範本裡用 $items 存取
        .fileName("合約書.docx")
        .build();
```

### 含圖片

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.XDOCREPORT)
        .outputFormat(OutputFormat.DOCX)
        .templatePath("templates/report-with-logo.docx")
        .fileName("report.docx")
        .parameters(Map.of("title", "月報"))
        .images(Map.of("logo", ImageSource.of(logoBytes, 200, 50)))
        .build();
```

---

## JasperReports 引擎

支援 `.jrxml` / `.jasper` 範本，產出 PDF 和 XLSX。

### 支援格式

| 格式 | 說明 |
|------|------|
| PDF | JasperReports 原生 PDF 輸出 |
| XLSX | JasperReports Excel 匯出 |

### 使用方式

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.JASPER)
        .outputFormat(OutputFormat.PDF)
        .templatePath("templates/invoice.jrxml")
        .fileName("invoice.pdf")
        .data(invoiceItems)
        .parameters(Map.of("title", "發票"))
        .build();

ReportResult result = reportService.generate(context);
```

> 建議開發階段用 `.jrxml`（方便修改），生產環境用 `.jasper`（省略編譯時間）。

---

## SPI 自訂引擎

### 自訂引擎範例

```java
@Component
public class MyCustomEngine implements ReportEngine {

    @Override
    public ReportEngineType getType() {
        return ReportEngineType.EXPORT;
    }

    @Override
    public ReportResult generate(ReportContext context) {
        // 自訂產製邏輯
    }

    @Override
    public boolean supports(OutputFormat format) {
        return format == OutputFormat.CSV;
    }
}
```

`ReportService` 會自動發現並路由到此引擎，不需要修改任何既有程式碼。

---

## 非同步產製

### 流程

```
1. POST /api/reports/generate-async
   → { "uuid": "abc-123" }

2. GET /api/reports/status/abc-123
   → { "status": "PROCESSING" }

3. GET /api/reports/status/abc-123
   → { "status": "COMPLETED", "fileName": "report.xlsx" }

4. GET /api/reports/download/abc-123
   → 下載檔案
```

### 在程式碼中使用

```java
// 建立 log 記錄
String uuid = logService.createLog("月報表", "monthly.xlsx");

// 非同步產製（背景執行）
asyncService.generateAsync(context, uuid);

// 回傳 UUID 給前端輪詢
return Map.of("uuid", uuid);
```

### 執行緒池配置

佇列滿時使用 `CallerRunsPolicy`（呼叫方執行緒接手），應用關閉時等待正在產製的報表完成（最多 60 秒）。

---

## 資料庫 Entity

| Entity | Table | 說明 |
|--------|-------|------|
| `ReportItem` | REPORT_ITEM | 報表定義（名稱、範本路徑、引擎類型） |
| `ReportLog` | REPORT_LOG | 產製記錄（UUID、狀態、開始/結束時間） |
| `ReportLogBlob` | REPORT_LOG_BLOB | 產製檔案（VARBINARY(MAX) 儲存） |

### 狀態機

```
PENDING → PROCESSING → COMPLETED
      ↘               ↘ FAILED

終態（COMPLETED / FAILED）不可再轉移。
```

### 索引

- `idx_report_log_uuid`（unique）— UUID 查詢
- `idx_report_log_status_created` — 狀態 + 建立時間複合索引（清理用）

---

## Pivot Table 記憶體優化

### 問題背景

樞紐分析表（Pivot Table）是硬需求。原始實作流程：

```
EasyExcel 串流寫入資料 → byte[] → new XSSFWorkbook(ByteArrayInputStream) → 加 Pivot → 輸出
```

EasyExcel 內部使用 SXSSFWorkbook（串流模式），記憶體友好。但加 Pivot 時必須用 XSSFWorkbook（DOM 模式）重新開啟整份檔案，**把串流省下的記憶體全部吐回去**。

### 原始方案記憶體分析（10 萬筆資料）

| 階段 | 物件 | 記憶體估算 |
|------|------|-----------|
| Java List | `context.getData()` — 10 萬個 Java 物件 | ~80-200 MB |
| EasyExcel 寫入 | `ByteArrayOutputStream` → `byte[]` | ~30-80 MB |
| Pivot 後處理 | `new XSSFWorkbook(new ByteArrayInputStream(excelBytes))` 整份 DOM 載入 | **~300-800 MB** |

POI 經驗法則：**1 MB xlsx ≈ 10 MB heap**。10 萬筆產出 ~50 MB xlsx，XSSFWorkbook 要吃 ~500 MB。

```
峰值記憶體（單一請求）:
  Java List:              ~150 MB
+ BAOS buffer:             ~50 MB
+ toByteArray 拷貝:        ~50 MB
+ BAIS 拷貝:               ~50 MB
+ XSSFWorkbook DOM:       ~500 MB
+ 輸出 byte[]:             ~50 MB
─────────────────────────────────
  合計:                   ~850 MB
```

### 優化方案：全程 Temp File + Memory-Mapped IO

核心思路：**有 Pivot 時，EasyExcel 直接寫到 temp file，整個流程不經過 ByteArrayOutputStream，避免 byte[] 重複拷貝**。

```java
// Step 1: EasyExcel 直接寫到 temp file
File tempFile = File.createTempFile("report-pivot-", ".xlsx");
try (FileOutputStream fos = new FileOutputStream(tempFile)) {
    writeExcelContent(context, fos);
}

// Step 2: OPCPackage.open(File) — memory-mapped IO，省 30-40%
try (OPCPackage pkg = OPCPackage.open(tempFile);
     XSSFWorkbook workbook = new XSSFWorkbook(pkg)) {
    addPivotTable(workbook, pivot);
    workbook.write(new FileOutputStream(tempFile));
}

// Step 3: 唯一一次 byte[] 拷貝
byte[] content = Files.readAllBytes(tempFile.toPath());
return new ReportResult(content, ...);
```

### 優化後記憶體比較（10 萬筆）

```
原始方案峰值:                     優化後峰值:
  Java List:        ~150 MB         Java List:        ~150 MB
+ BAOS buffer:       ~50 MB       + XSSFWorkbook DOM: ~350 MB (memory-mapped 省 30%)
+ toByteArray 拷貝:  ~50 MB       + readAllBytes:      ~50 MB
+ BAIS 拷貝:         ~50 MB       ──────────────────────────
+ XSSFWorkbook DOM: ~500 MB         合計:             ~550 MB
+ 輸出 byte[]:       ~50 MB
──────────────────────────
  合計:             ~850 MB         省了 ~300 MB（35%）
```

### 注意事項

- 需確保 temp 目錄（`java.io.tmpdir`）有足夠磁碟空間
- temp file 在 finally 區塊中刪除，異常也不會殘留
- WPS 對 Pivot Table 的相容性不完整，建議使用 Microsoft Excel 開啟

---

## 設計決策

| 要什麼 | 不要什麼 |
|--------|----------|
| SPI + 策略模式，引擎可插拔 | 寫死的 if-else 引擎判斷 |
| Builder 模式 (ReportContext) | 超長建構子或 Map 傳參 |
| core 不依賴 web，可在 batch job 使用 | 綁死 Controller 的報表邏輯 |
| @Version 樂觀鎖，防併發修改 | 無保護的狀態更新 |
| 路徑遍歷保護，禁止 `..`、`\`、`://` | 信任使用者傳入的路徑 |
| 狀態機驗證，終態不可再轉移 | 任意狀態跳轉 |
| 50MB 檔案限制，防 OOM | 無上限的檔案產製 |
| CallerRunsPolicy，佇列滿不丟棄 | DiscardPolicy 或 AbortPolicy |
| BLOB 分離表，查詢不載入大檔案 | 全部塞同一張表 |
| 批次 JPQL 刪除 | JPA deleteBy 逐筆刪除 |
| Temp file + memory-mapped IO | byte[] 多次拷貝 |

---

## 依賴關係

```
common-report-spring-boot-starter          ← 使用方引入這個
├── common-report-core                     ← SPI、Entity、Service
│   ├── common-jpa-spring-boot-starter     ← Entity 基類
│   └── common-response-spring-boot-starter ← BusinessException
└── common-report-autoconfigure            ← AutoConfiguration、Controller
    ├── common-report-core
    ├── spring-boot-starter-webmvc         ← REST API
    ├── spring-boot-starter-data-jpa       ← Entity 掃描
    ├── spring-boot-starter-data-redis     ← 限流
    └── springdoc-openapi-starter-webmvc-ui ← Swagger

common-report-engine-easyexcel             ← 引擎（獨立引入）
├── common-report-core
├── easyexcel 4.0.1
└── poi-ooxml 5.3.0

common-report-engine-xdocreport            ← 引擎（獨立引入）
├── common-report-core
├── xdocreport 2.1.0 (docx + velocity)
└── poi-ooxml 5.3.0

common-report-engine-jasper                ← 引擎（獨立引入）
├── common-report-core
└── jasperreports 7.0.1 (core + pdf + excel-poi)
```

---

## 專案結構與技術規格

### 目錄樹

```
common-report/
├── common-report-core/                     # 核心（純 library，無 web 依賴）
│   └── src/main/java/.../report/
│       ├── entity/                         ReportItem, ReportLog, ReportLogBlob
│       ├── enums/                          ReportEngineType, ReportStatus, OutputFormat
│       ├── repository/                     JPA Repository
│       ├── service/                        ReportService, ReportLogService,
│       │                                   ReportAsyncService, ReportThrottleService
│       └── spi/                            ReportEngine, ReportContext, ReportResult,
│                                           SheetData, PivotConfig, ImageSource
│
├── common-report-engine-easyexcel/         # EasyExcel 引擎
│   └── src/main/java/.../easyexcel/
│       ├── EasyExcelReportEngine.java      XLSX / XLS / CSV + Pivot Table
│       ├── CustomMergeStrategy.java        相同值自動垂直合併
│       └── EasyExcelAutoConfiguration.java
│
├── common-report-engine-xdocreport/        # xDocReport 引擎
│   └── src/main/java/.../xdocreport/
│       ├── XDocReportEngine.java           DOCX / PDF / ODT + 圖片
│       └── XDocReportAutoConfiguration.java
│
├── common-report-engine-jasper/            # JasperReports 引擎
│   └── src/main/java/.../jasper/
│       ├── JasperReportEngine.java         PDF / XLSX
│       └── JasperAutoConfiguration.java
│
├── common-report-autoconfigure/            # AutoConfiguration + Web 層
│   └── src/main/java/.../report/
│       ├── autoconfigure/                  ReportAutoConfiguration,
│       │                                   ReportAsyncConfiguration, ReportProperties
│       ├── controller/                     ReportController
│       └── dto/                            ReportGenerateRequest, ReportStatusResponse
│
├── common-report-spring-boot-starter/      # Starter 空殼（依賴聚合）
└── common-report-test/                     # 整合測試（7 Phase）
    └── src/test/java/.../test/
        ├── Phase1_EntityRepositoryTest
        ├── Phase2_ReportServiceTest
        ├── Phase3_ReportLogServiceTest
        ├── Phase4_EasyExcelEngineTest
        ├── Phase5_ReportControllerTest
        ├── Phase6_AutoConfigurationTest
        └── Phase7_ThrottleTest
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| EasyExcel | 4.0.1 |
| Apache POI | 5.3.0 |
| xDocReport | 2.1.0 |
| JasperReports | 7.0.1 |
| SpringDoc OpenAPI | 3.x |

### 引擎總覽

| 引擎 | 模組 | 支援格式 | 適合場景 | 狀態 |
|------|------|---------|---------|------|
| EasyExcel | `engine-easyexcel` | XLSX, XLS, CSV | 資料匯出、Excel 範本填充、Pivot Table | 完成 |
| xDocReport | `engine-xdocreport` | DOCX, PDF, ODT | Word 套表（合約、公文、通知書） | 完成 |
| JasperReports | `engine-jasper` | PDF, XLSX | 複雜版面報表（表頭、頁首頁尾） | 完成 |

### 注意事項：@EnableJpaRepositories

由於 `@EnableJpaRepositories` 是覆蓋式的，如果使用方自己也有 JPA Repository，需要在 `@SpringBootApplication` 上加：

```java
@SpringBootApplication
@EnableJpaRepositories(basePackages = "your.app.repository")
public class MyApplication { }
```

Report starter 的 entity/repository 會由 `ReportAutoConfiguration` 自動掃描，不需要手動加。

---

## 版本

### 1.0.0

- 初始版本
- EasyExcel 引擎：資料寫入、範本填充、多 Sheet、Pivot Table、CustomMergeStrategy
- xDocReport 引擎：DOCX 範本 + Velocity、圖片插入、PDF 轉換
- JasperReports 引擎：PDF / XLSX 輸出、jrxml 自動編譯
- 非同步產製：Spring @Async + ThreadPool + UUID 追蹤
- 審計記錄：ReportLog + ReportLogBlob + 狀態機
- Redis 分散式限流：ReportThrottleService
- REST API：同步/非同步產製、狀態查詢、下載
- 39+ 個整合測試（TDD Phase 1-7）
- Pivot Table 記憶體優化（temp file + memory-mapped IO）
