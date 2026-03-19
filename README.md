# Company Common Starters

公司共用 Spring Boot Starter 集合 — 從 wez-grails5（42 個模組）遷移而來，提供統一日誌、統一回應、JPA 審計、安全認證、附件管理、報表產製等功能。

引入即生效，零配置，各模組可獨立使用。

---

## 目錄

1. [模組總覽](#模組總覽)
2. [快速開始](#快速開始)
3. [使用情境](#使用情境)
4. [各模組功能](#各模組功能)
5. [配置參考](#配置參考)
6. [Grails 遷移狀態](#grails-遷移狀態)
7. [專案結構](#專案結構)
8. [開發指南](#開發指南)

---

## 模組總覽

| 模組 | 功能 | 說明 |
|------|------|------|
| `common-log-spring-boot-starter` | 統一日誌 | API 自動記錄 `-->` / `<--`、traceId 追蹤、敏感遮罩、慢 API 告警 |
| `common-response-spring-boot-starter` | 統一回應 | 自動包裝 `ApiResponse`、全局異常處理、錯誤碼體系 |
| `common-jpa-spring-boot-starter` | JPA 通用 | 自動審計（建立/修改時間+操作人）、軟刪除 |
| `common-attachment-spring-boot-starter` | 附件管理 | 檔案上傳/下載、Tika 類型偵測、DB/檔案系統雙模式 |
| `common-report-spring-boot-starter` | 報表產製 | 多引擎（EasyExcel + xDocReport + JasperReports）、非同步產製、審計記錄 |
| `common-notification-spring-boot-starter` | 通知系統 | 多通道（Email + WebSocket）、排程發送、失敗重試 |
| `care-security` | 安全模組 | JWT 認證、RBAC、LDAP、OTP、CAPTCHA、自然人憑證 |
| **業務級 Starter** | | |
| `common-signature-spring-boot-starter` | 電子簽名板 | Canvas JSON 存儲 + 附件圖片管理，依賴 attachment |

### 技術棧

| 項目 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 建置工具 | Maven 3.9+ |
| 資料庫 | Microsoft SQL Server |
| 快取 | Redis |

---

## 快速開始

### 1. 引入 BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.company.common</groupId>
            <artifactId>company-common-starters</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 要哪個加哪個（不用寫 version）

```xml
<dependencies>
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-log-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-response-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-jpa-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 3. 完成，不需要任何配置

---

## 使用情境

### 情境 A：一般 CRUD 後台

```xml
common-log + common-response + common-jpa
```

自動獲得：API 日誌、統一回應格式、Entity 審計欄位

### 情境 B：需要登入認證的系統

```xml
common-security-spring-boot-starter（已包含 log + response + jpa）
```

額外獲得：JWT 認證、RBAC 權限、可選 LDAP / OTP / CAPTCHA

### 情境 C：需要匯出報表

```xml
common-report-spring-boot-starter + common-report-engine-easyexcel
```

獲得：Excel/Word 匯出、非同步產製、產製記錄

### 情境 D：需要附件管理

```xml
common-attachment-spring-boot-starter
```

獲得：檔案上傳/下載、Tika 類型偵測、圖片壓縮

---

## 各模組功能

### common-log（統一日誌）

```
11:30:15.123 INFO  [d12e3e1f,08f395d1] ApiLogFilter : --> POST /api/auth/login body={username:admin, password:***}
11:30:15.789 INFO  [d12e3e1f,08f395d1] ApiLogFilter : <-- 200 POST /api/auth/login 274ms
```

| 功能 | 說明 |
|------|------|
| API 自動日誌 | `-->` 請求 / `<--` 回應，含方法、路徑、耗時 |
| traceId 追蹤 | Micrometer + OpenTelemetry，跨服務串聯 |
| 敏感遮罩 | password、token 等自動替換為 `***` |
| 慢 API 告警 | 超過閾值（預設 3s）自動 WARN + `[SLOW]` |
| `@Loggable` | 自訂單一 API 的日誌行為 |

### common-response（統一回應）

```json
{ "success": true, "code": "SUCCESS", "message": "Success", "data": { ... } }
{ "success": false, "code": "A0001", "message": "請求參數錯誤" }
```

| 功能 | 說明 |
|------|------|
| 自動包裝 | Controller 回傳值自動包成 `ApiResponse` |
| 全局異常處理 | 12 種異常自動轉標準錯誤回應 |
| 錯誤碼體系 | A（客戶端）/ B（業務）/ C（系統）/ D（外部） |
| BusinessException | `BusinessException.notFound("...")` 語意化拋出 |
| 可擴展 | 實作 `ErrorCode` 介面自訂錯誤碼 |

### common-jpa（JPA 審計 + 軟刪除）

```java
@Entity
public class Product extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // 自動獲得 createdDate, lastModifiedDate, createdBy, lastModifiedBy
}
```

| 功能 | 說明 |
|------|------|
| `AuditableEntity` | 繼承即有審計欄位 |
| `BaseEntity` | 繼承即有審計 + 軟刪除 + 樂觀鎖 |
| `SoftDeleteRepository` | `softDeleteById()` / `findAllActive()` / `restoreById()` |
| `DefaultAuditorAware` | 從 SecurityContext 自動取得當前使用者 |

### common-attachment（附件管理）

| 功能 | 說明 |
|------|------|
| 上傳/下載 | REST API，支援 multipart |
| 類型偵測 | Apache Tika 偵測真實 MIME type |
| 雙模式儲存 | 檔案系統 / DB BLOB 可切換 |
| 圖片壓縮 | 非同步壓縮，可配置最大尺寸 |
| 安全防護 | 副檔名黑名單、MIME type 白名單 |

### common-report（報表產製）

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.EASYEXCEL)
        .outputFormat(OutputFormat.XLSX)
        .fileName("employees.xlsx")
        .data(employeeList)
        .build();
ReportResult result = reportService.generate(context);
```

| 功能 | 說明 |
|------|------|
| EasyExcel 引擎 | XLSX / XLS / CSV 匯出（資料寫入 + 範本填充） |
| xDocReport 引擎 | DOCX / PDF（Word 範本 + Velocity 變數替換） |
| SPI 可插拔 | 加依賴自動註冊引擎，不改程式碼 |
| 非同步產製 | @Async + 狀態輪詢（PENDING → PROCESSING → COMPLETED） |
| 審計記錄 | ReportLog 自動記錄每次產製 |
| 安全防護 | 路徑遍歷保護、50MB 檔案上限、UUID 格式驗證 |

詳細文件：[common-report/README.md](common-report/README.md)

### care-security（安全模組）

| 功能 | 說明 |
|------|------|
| JWT 認證 | Access Token + Refresh Token，OAuth2 Authorization Server |
| RBAC | 角色 → 權限矩陣（Menu × CRUD） |
| 使用者管理 | CRUD、鎖定/解鎖、密碼重設 |
| LDAP | Active Directory / OpenLDAP 整合（可選） |
| OTP | TOTP 兩步驟驗證（可選） |
| CAPTCHA | 圖形驗證碼 + TTS 語音無障礙（可選） |
| 自然人憑證 | MOICA 數位簽章認證（可選） |
| 密碼政策 | 長度、複雜度、歷史密碼檢查 |
| 稽核日誌 | 登入記錄、操作日誌 |

詳細文件：[care-security/README.md](care-security/README.md)

---

## 配置參考

所有模組零配置即可用，以下為可選調整：

```yaml
# === 日誌 ===
common:
  log:
    enabled: true
    slow-threshold-ms: 3000
    mask-fields: [password, token, creditCard]
    exclude-patterns: [/actuator/**, /health]

# === 統一回應 ===
  response:
    enabled: true
    exclude-paths: [/actuator/**]

# === 報表 ===
  report:
    enabled: true
    async:
      enabled: true
      core-pool-size: 2
      max-pool-size: 5

# === 附件 ===
wez:
  attachment:
    storage-type: filesystem       # filesystem | database
    upload-path: ./uploads
    max-file-size: 10MB

# === 安全 ===
care:
  security:
    enabled: true
    jwt:
      access-token-ttl-minutes: 30
      refresh-token-ttl-days: 7
    login:
      max-attempts: 5
      lock-duration-minutes: 30
    ldap:
      enabled: false
    captcha:
      enabled: true
      audio-enabled: true
```

---

## Grails 遷移狀態

從 wez-grails5（42 個 Grails 模組）遷移到 Spring Boot Starter 的進度：

### 已完成（13 個）

| Grails 模組 | Spring Boot 對應 |
|---|---|
| `wez-logs` + `wez-logs-extra-filter` | `common-log-spring-boot-starter` |
| `wez-base-core`（Sauser, Role, Org） | `care-security` core |
| `wez-base`（基礎功能） | `common-jpa-spring-boot-starter` |
| `wez-security`（認證/權限） | `care-security` |
| `wez-web-security`（密碼變更/重設） | `care-security` AuthService |
| `wez-captcha` + `wez-security-captcha` | `auth-captcha` |
| `wez-security-auth-otp` | `auth-otp` |
| `wez-security-auth-moica-*`（2 個） | `auth-moica` |
| 統一回應/例外處理 | `common-response-spring-boot-starter` |
| `wez-attachment` | `common-attachment-spring-boot-starter` |
| `wez-report` + 4 引擎 | `common-report-spring-boot-starter` |
| `wez-notification` | `common-notification-spring-boot-starter` |
| `wez-diagram-sign` | `common-signature-spring-boot-starter`（business） |

### 原生取代（2 個）

| Grails 模組 | 替代方案 |
|---|---|
| `wez-cache-redis` | `spring-boot-starter-data-redis` + `@EnableCaching` |
| `wez-email` | `spring-boot-starter-mail` + `JavaMailSender` |

### 待建（6 個）

| Grails 模組 | 預計 Starter | 工作量 |
|---|---|---|
| `wez-crypto` | `common-crypto-starter` | 1-2 天 |
| `wez-ex-query` | `common-ex-query-starter` | 3-5 天 |
| `wez-api-hub` | `common-api-hub-starter` | 3-5 天 |
| `wez-security-auth-gca` | `auth-gca` | 3 天 |
| `wez-security-auth-moeaca` | `auth-moeaca` | 3 天 |
| `wez-security-auth-xca` | `auth-xca` | 3 天 |
| `wez-security-auth-health-card` | `auth-health-card` | 3 天 |

### 不需要遷移（21 個）

- **前後端分離淘汰**（3 個）：`wez-theme-bootstrap`、`wez-theme-webix`、`wez-web-bootstrap`
- **太專案化**（13 個）：`wez-board`、`wez-log-viewer`、`wez-portal`、`wez-system`、`wez-system-moica`、`wez-report-bug`、`wez-diagram-family`、`wez-web-api-hub` 等
- **獨立運作**（2 個）：`wez-test`、`websocket-proxy`（已是 Spring Boot 3.1.2）

詳細分析：[docs/migration-starter-analysis.md](docs/migration-starter-analysis.md)

---

## 專案結構

```
company-common-starters/
├── pom.xml                              ← Parent POM + BOM
├── company-build-tools/                 ← Checkstyle + SpotBugs 規則
├── common-log-spring-boot-starter/      ← 統一日誌
├── common-jpa-spring-boot-starter/      ← JPA 審計 + 軟刪除
├── common-response-spring-boot-starter/ ← 統一回應 + 例外處理
├── common-attachment-spring-boot-starter/ ← 附件管理
├── common-notification-spring-boot-starter/ ← 通知系統
├── common-report/                       ← 報表產製（多模組）
│   ├── common-report-core/              核心 SPI + Entity + Service
│   ├── common-report-engine-easyexcel/  EasyExcel 引擎
│   ├── common-report-engine-xdocreport/ xDocReport 引擎
│   ├── common-report-autoconfigure/     AutoConfiguration + Controller
│   ├── common-report-spring-boot-starter/ Starter 空殼
│   └── common-report-test/              39 個整合測試
├── care-security/                       ← 安全模組（多模組）
│   ├── common-security-core/            核心邏輯
│   ├── common-security-auth-ldap/       LDAP 認證
│   ├── common-security-auth-otp/        OTP 兩步驟驗證
│   ├── common-security-auth-captcha/    圖形驗證碼 + TTS
│   ├── common-security-auth-moica/      自然人憑證
│   ├── common-security-autoconfigure/   AutoConfiguration
│   ├── common-security-spring-boot-starter/ Starter 空殼
│   └── common-security-test/            整合測試
├── business/                            ← 業務級 Starter
│   └── common-signature-spring-boot-starter/ 電子簽名板
└── docs/                                ← 遷移文件
    ├── migration-starter-analysis.md    42 模組完整分析
    └── migration/                       各模組遷移指南
```

### 依賴關係

```
common-security-spring-boot-starter
└── common-security-core
    ├── common-log-spring-boot-starter      ← 獨立可用
    ├── common-response-spring-boot-starter ← 獨立可用
    └── common-jpa-spring-boot-starter      ← 獨立可用

common-report-spring-boot-starter           ← 獨立可用
common-attachment-spring-boot-starter       ← 獨立可用
common-notification-spring-boot-starter     ← 獨立可用

common-signature-spring-boot-starter       ← business
├── common-jpa-spring-boot-starter
└── common-attachment-spring-boot-starter
```

---

## 開發指南

### 本地建置

```bash
git clone https://github.com/yanchen184/company-common-starters.git
cd company-common-starters

mvn compile          # 編譯全部
mvn install          # 安裝到本地 Maven
mvn test             # 跑全部測試
```

### 展示專案

```bash
# 後端（需要 MSSQL + Redis）
git clone https://github.com/yanchen184/starter-showcase.git
cd starter-showcase && mvn spring-boot:run

# 前端
git clone https://github.com/yanchen184/security-starter-demo-frontend.git
cd security-starter-demo-frontend && npm install && npm run dev
```

### 靜態分析

專案內建 Checkstyle + SpotBugs，`mvn verify` 時自動執行：
- **Checkstyle**：程式碼風格檢查（規則在 `company-build-tools/checkstyle.xml`）
- **SpotBugs**：潛在 bug 偵測（High confidence）

另可用 IntelliJ `Analyze → Inspect Code` 做更全面的檢查（建議 scope 設為 `file:*.java&&!file:*/test/*`）。

### 版本管理

```bash
# 升版（全部模組一起改）
mvn versions:set -DnewVersion=1.1.0
mvn versions:commit
```

### 正式發佈流程

#### 1. 決定版號

| 改動類型 | 版號變更 | 範例 |
|----------|---------|------|
| Bug 修復、預設值調整 | patch | `1.0.0` → `1.0.1` |
| 新增功能（向下相容） | minor | `1.0.0` → `1.1.0` |
| 破壞性變更（API 改動） | major | `1.0.0` → `2.0.0` |

#### 2. 升版 + 建置 + 發佈

```bash
# 升版
mvn versions:set -DnewVersion=1.0.1
mvn versions:commit

# 建置 + 測試 + 靜態分析
mvn clean install

# 發佈到內部 Nexus（如有）
mvn deploy -DskipTests

# 提交 + 打 tag
git add -A
git commit -m "release: v1.0.1"
git tag v1.0.1
git push origin main --tags
```

#### 3. 消費端升級

消費端只需改 BOM 版號，所有 starter 版本統一跟著走：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.company.common</groupId>
            <artifactId>company-common-starters</artifactId>
            <version>1.0.1</version>  <!-- ← 改這一行 -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

然後重新 build：

```bash
mvn clean compile
```

不需要改個別 starter 的 version，BOM 統一管理。

#### 4. 版本紀錄

| 版本 | 日期 | 變更摘要 |
|------|------|---------|
| 1.0.0 | 2026-03-18 | 初始版本：log、response、jpa、security、attachment、report、notification、signature |

---

## 設計原則

| 原則 | 說明 |
|------|------|
| 零配置 | 加了 dependency 就能用，所有屬性都有合理預設值 |
| 可覆寫 | 所有 Bean 使用 `@ConditionalOnMissingBean`，使用方可自訂替換 |
| 可插拔 | 認證模組 / 報表引擎透過 SPI 介面解耦 |
| 依賴隔離 | 框架依賴用 `provided` scope，由使用方帶入 |
| core 不依賴 web | core 模組可在 batch job 使用，不強制帶入 web 層 |
