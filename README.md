# Company Common Starters

公司共用 Spring Boot Starter 集合 — 統一日誌、統一回應、JPA 通用模組、安全模組

引入即生效，零配置，各模組可獨立使用。

---

## 模組總覽

| 模組 | 功能 | 一句話說明 |
|------|------|-----------|
| [common-log-spring-boot-starter](common-log-spring-boot-starter/) | 統一日誌 | API 自動記錄 `-->` / `<--`、traceId 追蹤、敏感遮罩、慢 API 告警 |
| [common-response-spring-boot-starter](common-response-spring-boot-starter/) | 統一回應 | 自動包裝 `ApiResponse`、全局異常處理、錯誤碼體系 |
| [common-jpa-spring-boot-starter](common-jpa-spring-boot-starter/) | JPA 通用 | 自動審計（建立/修改時間+操作人）、可選軟刪除 |
| [care-security](care-security/) | 安全模組 | 認證授權、JWT、RBAC、LDAP、OTP、CAPTCHA |
| [starter-showcase](starter-showcase/) | 展示專案 | 乾淨的 Demo，體驗所有 starter 的效果 |

---

## 快速開始

### 1. 引入 BOM（版本統一管理）

在專案的 `pom.xml` 加入：

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
    <!-- 統一日誌 -->
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-log-spring-boot-starter</artifactId>
    </dependency>

    <!-- 統一回應 -->
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-response-spring-boot-starter</artifactId>
    </dependency>

    <!-- JPA 通用（審計、軟刪除） -->
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-jpa-spring-boot-starter</artifactId>
    </dependency>

    <!-- 安全模組（含 log + response + jpa） -->
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-security-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 3. 完成

不需要任何配置，引入即自動生效。

---

## 使用範例

### 場景 A：一般 CRUD 後台

只要日誌 + 統一回應 + JPA 審計：

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

加入後你的專案自動獲得：

```
✅ 所有 API 自動記錄 --> / <-- log（含 traceId）
✅ 成功回應自動包裝為 { success: true, code: "SUCCESS", data: {...} }
✅ 異常自動處理，回傳標準錯誤格式
✅ Entity 繼承 AuditableEntity 即有建立/修改時間+操作人
```

### 場景 B：需要認證授權的系統

只要加 security（它已經包含 log + response + jpa）：

```xml
<dependencies>
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-security-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

加入後額外獲得：

```
✅ 場景 A 的所有功能
✅ JWT 認證 + Refresh Token
✅ RBAC 角色權限控制
✅ LDAP 整合（可選）
✅ OTP / TOTP 兩步驟驗證（可選）
✅ CAPTCHA 驗證碼（可選）
```

### 場景 C：微服務只需要日誌

```xml
<dependencies>
    <dependency>
        <groupId>com.company.common</groupId>
        <artifactId>common-log-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

---

## BOM 是什麼？為什麼要用？

**BOM = 菜單**。列出所有 starter 的版本號，確保版本互相相容。

| | 沒有 BOM | 有 BOM |
|---|---|---|
| 寫法 | 每個 starter 都要寫 `<version>` | 只在 BOM 寫一次版本 |
| 升級 | 每個 starter 各自改版本，容易不一致 | 改 BOM 版本號就全部對齊 |
| 相容性 | 自己確保版本搭配 | BOM 內的版本已測試過相容 |

Spring Boot 的 `spring-boot-starter-parent` 就是 BOM — 所以你寫 `spring-boot-starter-web` 不用寫版本號。

---

## 各模組功能一覽

### common-log-spring-boot-starter

| 功能 | 說明 |
|------|------|
| API 自動日誌 | 所有 `@RestController` 自動記錄 `-->` 請求 / `<--` 回應 |
| traceId 追蹤 | Micrometer + OpenTelemetry，每筆 log 帶 `[traceId,spanId]` |
| 敏感遮罩 | password、token 等自動遮罩為 `***` |
| 慢 API 告警 | 超過閾值自動 WARN + `[SLOW]` 標記 |
| `@Loggable` | 自訂單一 API 的 log 行為 |

```
11:30:15.123 INFO  [d12e3e1f,08f395d1] ApiLogFilter : --> POST /api/auth/login body={username:admin, password:***}
11:30:15.789 INFO  [d12e3e1f,08f395d1] ApiLogFilter : <-- 200 POST /api/auth/login 274ms
```

### common-response-spring-boot-starter

| 功能 | 說明 |
|------|------|
| 統一回應格式 | 自動包裝為 `{ success, code, message, data }` |
| 全局異常處理 | 12 種異常自動轉標準錯誤回應 |
| 錯誤碼體系 | A（用戶端）/ B（業務）/ C（系統）/ D（外部） |
| BusinessException | 語意化拋出：`BusinessException.notFound("...")` |
| 路徑排除 | `/actuator/**` 等不走統一包裝 |

```json
// 成功
{ "success": true, "code": "SUCCESS", "message": "Success", "data": { "id": 1 } }

// 錯誤
{ "success": false, "code": "A0001", "message": "請求參數錯誤" }
```

### common-jpa-spring-boot-starter

| 功能 | 說明 |
|------|------|
| 自動審計 | 繼承 `AuditableEntity` 自動記錄建立/修改時間+操作人 |
| 軟刪除 | 繼承 `BaseEntity` + `SoftDeleteRepository`（opt-in） |
| AuditorAware | 從 SecurityContext 自動取當前使用者 |

```java
@Entity
public class Product extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // 自動獲得 createdDate, lastModifiedDate, createdBy, lastModifiedBy
}
```

### care-security（安全模組）

| 功能 | 說明 |
|------|------|
| JWT 認證 | OAuth2 Authorization Server + Resource Server |
| RBAC | 角色 → 權限，支援組織層級 |
| LDAP | 整合 OpenLDAP（可選） |
| OTP / TOTP | 兩步驟驗證（可選） |
| CAPTCHA | 圖形驗證碼（可選） |
| 密碼策略 | 可配置的密碼強度規則 |

---

## 配置參考

所有模組都是零配置（引入即生效），以下為可選調整：

```yaml
# 日誌模組
common:
  log:
    enabled: true
    slow-threshold-ms: 3000
    mask-fields:
      - password
      - token
      - creditCard
    log-response-body: false
    exclude-patterns:
      - /actuator/**
      - /health

# 統一回應模組
  response:
    enabled: true
    exclude-paths:
      - /actuator/**
```

---

## 依賴關係

```
common-security-spring-boot-starter
└── common-security-autoconfigure
    └── common-security-core
        ├── common-log-spring-boot-starter     ← 獨立可用
        ├── common-response-spring-boot-starter ← 獨立可用
        └── common-jpa-spring-boot-starter     ← 獨立可用
```

- 三個基礎 starter 互相獨立，可以單獨使用
- security-starter 包含所有三個基礎 starter，不需要重複引入

---

## 開發指南

### 環境需求

| 項目 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 4.0.3 |
| Maven | 3.9+ |

### 本地建置

```bash
# Clone
git clone https://github.com/yanchen184/company-common-starters.git
cd company-common-starters

# 編譯全部模組
mvn compile

# 安裝到本地 Maven（讓其他專案引用）
mvn install

# 只編譯某個模組
mvn compile -pl common-log-spring-boot-starter
```

### 專案結構

```
company-common-starters/
├── pom.xml                              ← Parent POM + BOM
├── common-log-spring-boot-starter/      ← 統一日誌
├── common-jpa-spring-boot-starter/      ← JPA 通用
├── common-response-spring-boot-starter/ ← 統一回應
├── care-security/                       ← 安全模組
│   ├── common-security-core/
│   ├── common-security-autoconfigure/
│   ├── common-security-spring-boot-starter/
│   └── common-security-test/
└── starter-showcase/                    ← Demo 展示專案（消費端範例）
```

### 版本管理

所有模組共用同一個版本號，由頂層 `pom.xml` 管理：

```bash
# 升版（全部模組一起改）
mvn versions:set -DnewVersion=1.1.0
mvn versions:commit
```

---

## 版本

- **1.0.0** — 初始版本
  - common-log-spring-boot-starter：Filter + Interceptor API 日誌、Micrometer Tracing、敏感遮罩
  - common-response-spring-boot-starter：ApiResponse 自動包裝、全局異常處理、錯誤碼體系
  - common-jpa-spring-boot-starter：自動審計、軟刪除、SoftDeleteRepository
  - care-security：JWT 認證、RBAC、LDAP、OTP、CAPTCHA、密碼策略
