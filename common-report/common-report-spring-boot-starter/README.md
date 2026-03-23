# Common Report Spring Boot Starter

報表產製 Starter — 依賴聚合模組，引入即自動載入報表核心功能。

---

## 快速開始

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-spring-boot-starter</artifactId>
</dependency>
```

此 Starter 聚合了：

- `common-report-core` — SPI 介面、Entity、Service
- `common-report-autoconfigure` — AutoConfiguration、REST API、配置屬性

### 選擇引擎

Starter 本身不包含引擎，需要額外引入（至少一個）：

```xml
<!-- Excel（XLSX / XLS / CSV + 樞紐分析表） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-easyexcel</artifactId>
</dependency>

<!-- Word / PDF（DOCX 範本 + Velocity） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-xdocreport</artifactId>
</dependency>

<!-- JasperReports（PDF / XLSX） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-jasper</artifactId>
</dependency>
```

引擎加入後自動註冊，零配置即可用。

### 完成

詳細文件請參考：[common-report/README.md](../README.md)

---

## 依賴關係

```
common-report-spring-boot-starter
├── common-report-core
└── common-report-autoconfigure
```
