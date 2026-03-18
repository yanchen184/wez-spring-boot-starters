# wez-report → common-report-spring-boot-starter

> 狀態：🟡 待建
> 預估工作量：5-7 天

## 模組結構

```
common-report/                               ← 聚合 POM
├── common-report-core/                      ← Entity、Repository、SPI、Service、Controller
├── common-report-engine-easyexcel/          ← EasyExcel 4.0.1 + POI 5.3.0
├── common-report-engine-jasper/             ← JasperReports 6.20.5
├── common-report-engine-xdocreport/         ← xDocReport 2.1.0 + Velocity
├── common-report-engine-export/             ← POI + OpenCSV + ODF（簡易列表匯出）
├── common-report-autoconfigure/             ← AutoConfiguration 集中管理
├── common-report-spring-boot-starter/       ← 空殼 Starter
└── common-report-test/                      ← 整合測試
```

## SPI 介面

```java
public interface ReportEngine {
    ReportEngineType getType();
    ReportResult generate(ReportContext context);
    boolean supports(OutputFormat format);
}
```

ReportService 收集所有 ReportEngine Bean，依 engineType 路由。

## 配置項

```yaml
common:
  report:
    enabled: true
    storage:
      type: database            # database | filesystem
      path: /tmp/reports
      max-file-size: 100MB
    async:
      enabled: true
      core-pool-size: 2
      max-pool-size: 5
    transfer:
      enabled: false            # JodConverter 轉檔（需要 LibreOffice）
      libre-office-home: /usr/lib/libreoffice
    cleanup:
      enabled: true
      retention-days: 90
      cron: "0 0 2 * * ?"
    engines:
      easyexcel:
        enabled: true
      jasper:
        enabled: true
      xdocreport:
        enabled: true
      export:
        enabled: true
```

## 使用方式

```xml
<!-- 基礎 -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-spring-boot-starter</artifactId>
</dependency>

<!-- 選擇需要的引擎 -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-easyexcel</artifactId>
</dependency>
```

## 開發順序

1. common-report-core（Entity/SPI/Service/Controller）
2. common-report-autoconfigure（AutoConfiguration）
3. common-report-spring-boot-starter（空殼）
4. common-report-engine-easyexcel（最常用）
5. common-report-engine-export（簡易匯出）
6. common-report-engine-jasper（複雜報表）
7. common-report-engine-xdocreport（Word 套表）
8. common-report-test（整合測試）

## 第三方依賴版本

| 依賴 | 版本 |
|------|------|
| EasyExcel | 4.0.1 |
| Apache POI | 5.3.0 |
| JasperReports | 6.20.5 |
| xDocReport | 2.1.0 |
| OpenCSV | 5.9 |
| JodConverter | 4.4.7 |
| OpenPDF | 1.3.30 |

## 注意事項

- POI 版本需統一鎖定（EasyExcel 和 JasperReports 都依賴）
- JasperReports 6.x 在 Java 21 需加 `--add-opens` JVM 參數
- JodConverter 需要安裝 LibreOffice，預設關閉
- 大檔案建議用 filesystem 模式，不存 DB BLOB
