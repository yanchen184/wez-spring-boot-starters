# Common Report Engine Jasper

JasperReports 報表引擎 — 支援 .jrxml / .jasper 範本，PDF 和 XLSX 輸出。

---

## 快速開始

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-jasper</artifactId>
</dependency>
```

加入後自動註冊為 `ReportEngine`，`ReportService` 會自動派發 `JASPER` 類型的請求。

### 最小範例

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

### 完成

無需額外配置，引擎自動生效。

---

## 功能總覽

- **自動編譯** — `.jrxml` 自動編譯為 JasperReport，無需預編譯
- **預編譯支援** — `.jasper` 檔案直接使用，跳過編譯步驟
- **JRBeanCollectionDataSource** — 直接從 Java `List<?>` 填入資料
- **字體 fallback** — 禁用 AWT 嚴格字體檢查，避免伺服器缺字體報錯
- **多格式輸出** — PDF、XLSX
- **50MB 上限** — 產製結果超過上限自動拋出 `BusinessException`

---

## 核心 API

### 支援格式

| 格式 | 說明 |
|------|------|
| PDF | JasperReports 原生 PDF 輸出 |
| XLSX | JasperReports Excel 匯出 |

### 基本範例

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

### 範本格式

| 副檔名 | 說明 |
|--------|------|
| `.jrxml` | XML 原始檔，引擎自動編譯 |
| `.jasper` | 預編譯檔，跳過編譯直接使用 |

> 建議開發階段用 `.jrxml`（方便修改），生產環境用 `.jasper`（省略編譯時間）。

---

## 配置

零配置。引擎加入依賴後自動註冊，無需任何配置屬性。

---

## 設計決策

| 要什麼 | 不要什麼 |
|--------|----------|
| 自動偵測 jrxml/jasper 格式 | 手動指定編譯方式 |
| JRBeanCollectionDataSource | 要求使用方建立 DataSource |
| 禁用 AWT 嚴格字體檢查 | 伺服器必須安裝所有字體 |
| 50MB 檔案上限防 OOM | 無限制的檔案產製 |

---

## 依賴關係

```
common-report-engine-jasper
├── common-report-core
├── jasperreports 7.0.1
├── jasperreports-pdf 7.0.1
├── jasperreports-excel-poi 7.0.1
└── spring-boot-autoconfigure
```

---

## 專案結構與技術規格

### 目錄樹

```
common-report-engine-jasper/
└── src/main/java/com/company/common/report/engine/jasper/
    ├── JasperReportEngine.java             # 引擎主類（PDF/XLSX）
    └── JasperAutoConfiguration.java        # 自動配置
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| JasperReports | 7.0.1 |
| 檔案數 | 2 個 Java 類別 |

---

## 版本

### 1.0.0

- 初始版本
- PDF / XLSX 輸出
- jrxml 自動編譯 + jasper 預編譯支援
- JRBeanCollectionDataSource 資料填入
- 字體 fallback 機制
