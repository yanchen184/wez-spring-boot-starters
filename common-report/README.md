# common-report-spring-boot-starter

Spring Boot 4.0 報表產製 Starter，提供多引擎支援、非同步產製、審計記錄，加依賴即可使用。

從 wez-grails5 的 5 個報表模組（wez-report / wez-report-easyexcel / wez-report-xdocreport / wez-report-ireport / wez-report-grails-export）遷移而來，在保留原有功能的基礎上增加了 SPI 引擎架構、狀態機管理、安全防護等企業級特性。

## 技術棧

- Spring Boot 4.0.3 / Java 21
- EasyExcel 4.0.1 + Apache POI 5.3.0
- xDocReport 2.1.0 + Velocity
- Spring Data JPA（審計記錄）
- Spring Async（非同步產製）
- SpringDoc OpenAPI 3（Swagger UI）

---

## 目錄

1. [快速開始](#快速開始)
2. [模組結構](#模組結構)
3. [引擎總覽](#引擎總覽)
4. [REST API](#rest-api)
5. [EasyExcel 引擎](#easyexcel-引擎)
6. [xDocReport 引擎](#xdocreport-引擎)
7. [SPI 自訂引擎](#spi-自訂引擎)
8. [非同步產製](#非同步產製)
9. [配置項](#配置項)
10. [資料庫 Entity](#資料庫-entity)
11. [與 Grails 版的差異](#與-grails-版的差異)
12. [待開發功能](#待開發功能)
13. [注意事項](#注意事項)
14. [測試](#測試)
15. [設計決策](#設計決策)
16. [Pivot Table 記憶體優化](#pivot-table-記憶體優化)

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

### 2. 零配置即可用

引入依賴後，以下功能自動生效：

- `ReportService` — 報表產製（依引擎類型自動派發）
- `ReportLogService` — 審計記錄（自動記錄每次產製）
- `ReportAsyncService` — 非同步產製
- `ReportController` — REST API（產製 / 狀態查詢 / 下載）

### 3. 在程式碼中使用

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

---

## 模組結構

```
common-report/
├── common-report-core/                  # 核心（純 library，無 web 依賴）
│   ├── entity/                          ReportItem, ReportLog, ReportLogBlob
│   ├── enums/                           ReportEngineType, ReportStatus, OutputFormat
│   ├── repository/                      JPA Repository
│   ├── service/                         ReportService, ReportLogService, ReportAsyncService
│   └── spi/                             ReportEngine(SPI), ReportContext, ReportResult
│
├── common-report-engine-easyexcel/      # EasyExcel 引擎
│   ├── EasyExcelReportEngine.java       XLSX / XLS / CSV
│   ├── CustomMergeStrategy.java         相同值自動垂直合併
│   └── EasyExcelAutoConfiguration.java
│
├── common-report-engine-xdocreport/     # xDocReport 引擎
│   ├── XDocReportEngine.java            DOCX / PDF / ODT
│   └── XDocReportAutoConfiguration.java
│
├── common-report-autoconfigure/         # AutoConfiguration + Web 層
│   ├── ReportAutoConfiguration.java     核心 Bean 註冊
│   ├── ReportAsyncConfiguration.java    @EnableAsync + ThreadPool
│   ├── ReportProperties.java            配置項
│   ├── ReportController.java            REST API
│   └── dto/                             Request / Response DTO
│
├── common-report-spring-boot-starter/   # Starter 空殼
└── common-report-test/                  # 39 個整合測試（TDD Phase 1-6）
```

---

## 引擎總覽

| 引擎 | 模組 | 支援格式 | 適合場景 | 狀態 |
|------|------|---------|---------|------|
| EasyExcel | `engine-easyexcel` | XLSX, XLS, CSV | 資料匯出、Excel 範本填充 | ✅ 完成 |
| xDocReport | `engine-xdocreport` | DOCX, PDF, ODT | Word 套表（合約、公文、通知書） | ✅ 完成 |
| JasperReports | `engine-jasper` | PDF, XLS, HTML | 複雜版面報表（表頭、頁首頁尾） | ❓ 待確認需求 |
| 簡易匯出 | `engine-export` | CSV, ODS, XML | POI 列表匯出 | ❓ 待確認需求 |

引擎是**可插拔**的 — 加依賴自動註冊，不用改任何程式碼。

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

---

## EasyExcel 引擎

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

### 支援格式

| OutputFormat | 說明 |
|-------------|------|
| `DOCX` | 直接輸出 Word |
| `PDF` | Word → PDF（需要 xdocreport converter） |
| `ODT` | OpenDocument Text |

---

## SPI 自訂引擎

### ReportEngine 介面

```java
public interface ReportEngine {
    ReportEngineType getType();
    ReportResult generate(ReportContext context);
    boolean supports(OutputFormat format);
}
```

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

```yaml
common:
  report:
    async:
      enabled: true
      core-pool-size: 2
      max-pool-size: 5
      queue-capacity: 100
```

佇列滿時使用 `CallerRunsPolicy`（呼叫方執行緒接手），應用關閉時等待正在產製的報表完成（最多 60 秒）。

---

## 配置項

所有配置前綴：`common.report`

```yaml
common:
  report:
    enabled: true                    # 總開關（預設 true）

    storage:
      type: database                 # database | filesystem
      path: /tmp/reports             # filesystem 時的儲存路徑

    async:
      enabled: true                  # 啟用非同步產製
      core-pool-size: 2              # 核心執行緒數
      max-pool-size: 5               # 最大執行緒數
      queue-capacity: 100            # 佇列容量

    cleanup:
      enabled: false                 # 啟用定期清理舊記錄
      retention-days: 90             # 保留天數（清理 COMPLETED + FAILED）
```

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

## 與 Grails 版的差異

### 保留的功能（從 Grails 搬過來）

| 功能 | Grails | Spring Boot |
|------|--------|-------------|
| Excel 匯出 | EasyExcel 4.0.1 | ✅ 一樣 |
| Word 套表 | xDocReport + Velocity | ✅ 一樣 |
| 報表記錄 | ReportLog + ReportLogBlob | ✅ 一樣 |
| 合併儲存格 | ColumnValueAutoMergeStrategy | ✅ CustomMergeStrategy（效能優化版） |
| 非同步產製 | Grails Promises API | ✅ Spring @Async + ThreadPool |

### 新增的功能（Grails 沒有）

| 功能 | 說明 |
|------|------|
| SPI 引擎插拔 | `ReportEngine` 介面 + 策略模式，加依賴自動註冊 |
| Builder 模式 | `ReportContext.builder()` 流暢 API（Grails 用 Map 傳參） |
| 狀態機 | PENDING → PROCESSING → COMPLETED/FAILED，有轉移驗證 |
| 樂觀鎖 | `@Version` 防止併發修改 ReportLog |
| UUID 下載 | 非同步完成後用 UUID 下載（Grails 用 Session 存） |
| REST API | 完整的 Controller（Grails 是 GSP 頁面） |
| 路徑遍歷保護 | templatePath 驗證 `..`、`\`、`://`、`%` |
| 檔案大小限制 | 50MB 上限保護，防 OOM |
| 批次清理 | JPQL 批次刪除（先刪 blob 再刪 log），也清理 FAILED 記錄 |
| Thread Pool 優雅關閉 | CallerRunsPolicy + waitForTasksToComplete |
| UUID 格式驗證 | 防止亂字串打 DB 的 DoS 攻擊 |
| core 不依賴 web | 可在 batch job 使用（Grails 綁死 Controller） |
| AutoConfiguration | 零配置，`@ConditionalOnClass` 自動偵測引擎 |
| 39 個 TDD 測試 | Grails 版完全沒有測試 |

---

## 待開發功能

| 功能 | 工作量 | 前提 | 說明 |
|------|--------|------|------|
| JodConverter 轉檔 | 半天 | 伺服器要裝 LibreOffice | XLSX → PDF、DOCX → PDF，配置已預留 `common.report.transfer` |
| ODS 匯出 | 半天 | 加 `simple-odf` 依賴 | OpenDocument Spreadsheet，使用場景少 |
| JasperReports 引擎 | 2-3 天 | 確認有在用 `.jrxml` | 複雜版面報表，可能是獨立服務而非嵌入式 |
| Scheduled 自動清理 | 2 小時 | — | 用 `@Scheduled` + cron 定時跑 cleanup |
| Metrics 監控 | 2 小時 | Micrometer | 產製次數、耗時、失敗率 |

### JodConverter 轉檔（待開發）

配置已預留，實作後啟用即可：

```yaml
common:
  report:
    transfer:
      enabled: true
      libre-office-home: /usr/lib/libreoffice
```

用途：產製完的 XLSX/DOCX 自動轉成 PDF，適合需要列印或歸檔的場景。

### Scheduled 自動清理（待開發）

配置已預留：

```yaml
common:
  report:
    cleanup:
      enabled: true
      retention-days: 90
      cron: "0 0 2 * * ?"    # 每天凌晨 2 點
```

---

## 注意事項

### @EnableJpaRepositories

由於 `@EnableJpaRepositories` 是覆蓋式的，如果使用方自己也有 JPA Repository，需要在 `@SpringBootApplication` 上加：

```java
@SpringBootApplication
@EnableJpaRepositories(basePackages = "your.app.repository")
public class MyApplication { }
```

Report starter 的 entity/repository 會由 `ReportAutoConfiguration` 自動掃描，不需要手動加。

### 檔案下載與 GlobalResponseAdvice

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

## 測試

39 個整合測試，TDD 規格文件風格：

| Phase | 測試內容 | 數量 |
|-------|---------|------|
| 1 | Entity / Repository | 7 |
| 2 | ReportService（引擎派發 + 路徑驗證） | 6 |
| 3 | ReportLogService（狀態機 + 審計） | 10 |
| 4 | EasyExcelReportEngine | 7 |
| 5 | ReportController（HTTP API） | 4 |
| 6 | AutoConfiguration（Bean 載入） | 5 |

```bash
cd common-report
mvn test -pl common-report-test
```

---

## 設計決策

| 決策 | 理由 |
|------|------|
| core 不依賴 web | 可在 batch job 中使用，不強制帶入 web 層 |
| SPI + 策略模式 | 引擎可插拔，加依賴自動註冊，不改既有程式碼 |
| Builder 模式 (ReportContext) | 參數多且可選，Builder 比 constructor 清晰 |
| @Version 樂觀鎖 | 非同步場景防止併發修改 ReportLog |
| 路徑遍歷保護 | templatePath 禁止 `..`、`\`、`://`、`%` |
| 狀態機驗證 | 終態不可再轉移，防止資料不一致 |
| 50MB 檔案限制 | 防止 OOM，保護 JVM |
| CallerRunsPolicy | 佇列滿時呼叫方執行緒接手，不丟棄任務 |
| BLOB 分離表 | ReportLogBlob 獨立，查詢 ReportLog 不會載入大型檔案 |
| 批次 JPQL 刪除 | 避免 JPA deleteBy 逐筆刪除的效能問題 |
| Pivot Table temp file 優化 | 避免 byte[] 重複拷貝 + memory-mapped IO，詳見下方 |

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
| ① Java List | `context.getData()` — 10 萬個 Java 物件 | ~80-200 MB |
| ② EasyExcel 寫入 | `ByteArrayOutputStream` → `byte[]` | ~30-80 MB |
| ③ Pivot 後處理 | `new XSSFWorkbook(new ByteArrayInputStream(excelBytes))` 整份 DOM 載入 | **~300-800 MB** |

POI 經驗法則：**1 MB xlsx ≈ 10 MB heap**。10 萬筆產出 ~50 MB xlsx，XSSFWorkbook 要吃 ~500 MB。

```
峰值記憶體（單一請求）:
  Java List:              ~150 MB
+ EasyExcel byte[]:        ~50 MB
+ byte[] 複製到 BAIS:      ~50 MB
+ XSSFWorkbook DOM:       ~500 MB
+ 輸出 byte[]:             ~50 MB
─────────────────────────────────
  合計:                   ~800 MB
```

如果同時 2-3 人跑報表（maxPoolSize=5），直接 OOM。

### 優化方案：Temp File + Memory-Mapped IO

核心思路：**用 temp file 取代 ByteArrayInputStream，並提早釋放 byte[] 讓 GC 回收**。

```java
File tempFile = File.createTempFile("report-pivot-", ".xlsx");
Files.write(tempFile.toPath(), excelBytes);
excelBytes = null; // 提早釋放 ~50MB 讓 GC 回收

try (OPCPackage pkg = OPCPackage.open(tempFile);       // memory-mapped IO
     XSSFWorkbook workbook = new XSSFWorkbook(pkg)) {
    // 加入 Pivot Table...
}
```

為什麼有效：
1. **`excelBytes = null`** — byte[] 提早釋放，省 ~50 MB
2. **`OPCPackage.open(File)`** — 使用 memory-mapped IO，比 `open(InputStream)` 省 30-40% 記憶體（InputStream 方式會先把整個 stream 複製到記憶體再解壓）
3. **程式碼改動極小** — 只改 `addPivotTables()` 方法，原有的 `addPivotTable()` Pivot 建立邏輯完全不動

### 優化後記憶體比較（10 萬筆）

```
峰值記憶體（優化後）:
  Java List:              ~150 MB
+ XSSFWorkbook DOM:       ~350 MB  (memory-mapped 省 30%)
+ 輸出 byte[]:             ~50 MB
─────────────────────────────────
  合計:                   ~550 MB  (原本 ~800 MB，省 ~250 MB)
```

### 為什麼不用更激進的方案

曾評估過 SAX + 迷你 XSSFWorkbook + OPC XML 移植的方案（可將記憶體降至 ~3 MB），但：
- 程式碼從 ~70 行暴增到 ~770 行，維護成本過高
- 直接操作 OOXML 結構，對 POI 版本升級敏感
- workbook.xml 用字串替換，namespace 變動就壞
- 沒有 fallback 機制，失敗時整個報表產製掛掉

**Temp file 方案改動 5 行，省 30% 記憶體，零維護風險。** 搭配 throttle 限制大報表並發數即可。

### 注意事項

- 需確保 temp 目錄（`java.io.tmpdir`）有足夠磁碟空間
- temp file 在 finally 區塊中刪除，異常也不會殘留
- WPS 對 Pivot Table 的相容性不完整，建議使用 Microsoft Excel 開啟
