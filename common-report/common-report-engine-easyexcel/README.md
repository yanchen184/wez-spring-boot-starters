# Common Report Engine EasyExcel

EasyExcel 報表引擎 — Excel 匯出（XLSX / XLS / CSV），支援樞紐分析表、自動合併、多 Sheet。

---

## 快速開始

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-easyexcel</artifactId>
</dependency>
```

加入後自動註冊為 `ReportEngine`，`ReportService` 會自動派發 `EASYEXCEL` 類型的請求。

### 最小範例

```java
ReportContext context = ReportContext.builder()
    .engineType(ReportEngineType.EASYEXCEL)
    .outputFormat(OutputFormat.XLSX)
    .fileName("users.xlsx")
    .data(userList)
    .build();

ReportResult result = reportService.generate(context);
```

### 完成

無需額外配置，引擎自動生效。

---

## 功能總覽

- **資料寫入模式** — 傳入 `List<?>` 直接產生 Excel
- **範本填充模式** — 提供 `.xlsx` 範本，用變數填充
- **多 Sheet 模式** — 多個 `SheetData` 各佔一個 Sheet
- **樞紐分析表模式** — `PivotConfig` 自動建立 Pivot Table
- **自動合併** — `CustomMergeStrategy` 相同值自動垂直合併儲存格
- **多報表合併** — `generateMerged()` 支援多個 context 合併為一個檔案
- **Memory-mapped IO** — Pivot Table 使用 temp file + `OPCPackage.open(File)` 降低記憶體
- **50MB 上限** — 產製結果超過上限自動拋出 `BusinessException`

---

## 核心 API

### 支援格式

| 格式 | 說明 |
|------|------|
| XLSX | Excel 2007+（含 Pivot Table 支援） |
| XLS | Excel 97-2003 |
| CSV | 逗號分隔值 |

### 1. 資料寫入模式

```java
ReportContext context = ReportContext.builder()
    .engineType(ReportEngineType.EASYEXCEL)
    .outputFormat(OutputFormat.XLSX)
    .fileName("users.xlsx")
    .data(userList)                    // List<UserDTO>
    .headClass(UserDTO.class)          // EasyExcel @ExcelProperty 標注
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

### 2. 範本填充模式

```java
ReportContext context = ReportContext.builder()
    .engineType(ReportEngineType.EASYEXCEL)
    .outputFormat(OutputFormat.XLSX)
    .templatePath("templates/invoice.xlsx")
    .fileName("invoice.xlsx")
    .parameters(Map.of("company", "ABC Corp", "date", "2026-03-23"))
    .data(lineItems)                   // 填入範本的 {.field} 區塊
    .build();
```

### 3. 多 Sheet 模式

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

### 4. 樞紐分析表模式

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

## 配置

零配置。引擎加入依賴後自動註冊，無需任何配置屬性。

---

## 設計決策

| 要什麼 | 不要什麼 |
|--------|----------|
| Temp file + memory-mapped IO | byte[] 多次拷貝 |
| 50MB 檔案上限防 OOM | 無限制的檔案產製 |
| EasyExcel 串流寫入 | 全部載入記憶體再寫出 |
| 自動偵測 headClass 產生欄位 | 手動定義每一欄 |

---

## 依賴關係

```
common-report-engine-easyexcel
├── common-report-core
├── easyexcel 4.0.1
├── poi-ooxml 5.3.0
└── spring-boot-autoconfigure
```

---

## 專案結構與技術規格

### 目錄樹

```
common-report-engine-easyexcel/
└── src/main/java/com/company/common/report/engine/easyexcel/
    ├── EasyExcelReportEngine.java          # 引擎主類（XLSX/XLS/CSV + Pivot）
    ├── CustomMergeStrategy.java            # 相同值自動垂直合併
    └── EasyExcelAutoConfiguration.java     # 自動配置
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| EasyExcel | 4.0.1 |
| Apache POI | 5.3.0 |
| 檔案數 | 3 個 Java 類別 |

---

## 版本

### 1.0.0

- 初始版本
- 四種產製模式：資料寫入、範本填充、多 Sheet、樞紐分析表
- CustomMergeStrategy 自動垂直合併
- Pivot Table 記憶體優化（temp file + memory-mapped IO）
- generateMerged() 多報表合併
