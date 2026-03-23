# Common Report Engine XDocReport

xDocReport 報表引擎 — Word 範本填充 + PDF 轉換，支援圖片插入。

---

## 快速開始

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-xdocreport</artifactId>
</dependency>
```

加入後自動註冊為 `ReportEngine`，`ReportService` 會自動派發 `XDOCREPORT` 類型的請求。

### 最小範例

```java
ReportContext context = ReportContext.builder()
    .engineType(ReportEngineType.XDOCREPORT)
    .outputFormat(OutputFormat.DOCX)
    .templatePath("templates/contract.docx")
    .fileName("contract.docx")
    .parameters(Map.of("companyName", "ABC Corp"))
    .build();

ReportResult result = reportService.generate(context);
```

### 完成

無需額外配置，引擎自動生效。

---

## 功能總覽

- **Velocity 模板引擎** — DOCX 範本中使用 Velocity 語法替換變數
- **List 迴圈** — 範本內支援表格行重複
- **圖片插入** — 透過 POI 後處理插入圖片到指定書籤
- **PDF 轉換** — 內建 DOCX → PDF converter
- **多格式輸出** — DOCX、PDF、ODT
- **50MB 上限** — 產製結果超過上限自動拋出 `BusinessException`

---

## 核心 API

### 支援格式

| OutputFormat | 說明 |
|-------------|------|
| `DOCX` | 直接輸出 Word |
| `PDF` | DOCX → PDF 轉換（需要 xdocreport converter） |
| `ODT` | OpenDocument Text |

### 基本範例（參數替換 + List 迴圈）

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

ReportResult result = reportService.generate(context);
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

### 範本語法

使用 Velocity 模板引擎，在 `.docx` 範本中寫入：

| 語法 | 說明 |
|------|------|
| `${field}` | 單一欄位替換 |
| `[#list items as item]...[/#list]` | List 迴圈 |
| `[#if condition]...[/#if]` | 條件判斷 |

範本範例：

```
合約書

甲方：$clientName
日期：$contractDate

明細：
#foreach($item in $items)
  - $item.name：$item.amount 元
#end
```

---

## 配置

零配置。引擎加入依賴後自動註冊，無需任何配置屬性。

---

## 設計決策

| 要什麼 | 不要什麼 |
|--------|----------|
| Velocity 模板語法 | 自製模板語法 |
| POI 後處理插入圖片 | xDocReport 原生圖片（限制多） |
| 50MB 檔案上限防 OOM | 無限制的檔案產製 |
| 自動清理空表格行 | 留下範本殘留的空行 |

---

## 依賴關係

```
common-report-engine-xdocreport
├── common-report-core
├── fr.opensagres.xdocreport.document.docx     ← DOCX 範本支援
├── fr.opensagres.xdocreport.template.velocity  ← Velocity 模板引擎
├── fr.opensagres.xdocreport.converter.docx.xwpf ← PDF 轉換（provided）
├── poi-ooxml 5.3.0                             ← 圖片後處理
└── spring-boot-autoconfigure
```

---

## 專案結構與技術規格

### 目錄樹

```
common-report-engine-xdocreport/
└── src/main/java/com/company/common/report/engine/xdocreport/
    ├── XDocReportEngine.java               # 引擎主類（DOCX/PDF/ODT + 圖片）
    └── XDocReportAutoConfiguration.java    # 自動配置
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| xDocReport | 2.1.0 |
| Apache POI | 5.3.0 |
| 檔案數 | 2 個 Java 類別 |

---

## 版本

### 1.0.0

- 初始版本
- Velocity 模板引擎：參數替換、List 迴圈、條件判斷
- 圖片插入：POI 後處理書籤替換
- PDF 轉換：DOCX → PDF
- 自動清理空表格行
