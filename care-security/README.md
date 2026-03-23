# Common Security Spring Boot Starter

安全框架模組 — JWT 認證、RBAC 權限、帳號管理、可選 LDAP / OTP / CAPTCHA / 自然人憑證

---

## 目錄

1. [加入後你的專案自動獲得](#加入後你的專案自動獲得)
2. [快速開始](#快速開始)
3. [功能總覽](#功能總覽)
4. [核心 API](#核心-api)
5. [配置](#配置)
6. [設計決策](#設計決策)
7. [依賴關係](#依賴關係)
8. [專案結構與技術規格](#專案結構與技術規格)
9. [版本](#版本)

---

## 加入後你的專案自動獲得

| 功能 | 加入前 | 加入後 |
|------|--------|--------|
| JWT 認證 | 需要自己整合 Spring Security + JWT | Access Token + Refresh Token + 黑名單，開箱即用 |
| 權限控制 | 需要自己設計角色權限表 | RBAC 權限矩陣（Menu × CRUD），支援 `@PreAuthorize` |
| 使用者管理 | 需要自己寫 CRUD | REST API 含建立、鎖定/解鎖、重設密碼 |
| 密碼安全 | 需要自己處理加密和政策 | SHA-512 加密 + 密碼歷史 + 複雜度/長度政策 |
| 登入保護 | 沒有暴力破解防護 | 失敗鎖定（Redis 計數）+ CAPTCHA + OTP 可選 |
| 稽核日誌 | 需要自己記錄 | 登入紀錄 + 操作日誌自動記錄 |
| Swagger UI | 需要自己配置 Bearer Token | 自動配置 SpringDoc OpenAPI + Bearer 認證 |
| 可選認證 | — | LDAP、OTP、CAPTCHA、自然人憑證，加依賴即啟用 |

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-security-spring-boot-starter</artifactId>
</dependency>
```

### 2. 配置資料庫和 Redis

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=care_security
  data:
    redis:
      host: localhost
      port: 6379

care:
  security:
    jwt:
      access-token-ttl-minutes: 30
      refresh-token-ttl-days: 7
    cors:
      allowed-origins: http://localhost:3000
```

### 3. 完成

REST API 自動註冊（`/api/auth/*`、`/api/users/*`、`/api/roles/*` 等）。

> Demo 專案：[security-starter-demo](https://github.com/yanchen184/security-starter-demo)

---

## 功能總覽

- **JWT 認證** — OAuth2 Authorization Server，Access + Refresh Token，Token 黑名單（登出即失效）
- **RBAC 權限** — 角色 → 權限矩陣（Menu × CRUD），支援 `@PreAuthorize("hasPermission('SC900','READ')")`
- **使用者管理** — CRUD、鎖定/解鎖、重設密碼、密碼歷史檢查
- **密碼政策** — 長度、複雜度、歷史密碼、到期提醒
- **登入保護** — 失敗次數追蹤 + 帳號鎖定（Redis）
- **稽核日誌** — 登入紀錄、操作日誌
- **LDAP** — Active Directory / OpenLDAP 整合（可選）
- **OTP** — TOTP 兩步驟驗證（可選）
- **CAPTCHA** — 圖形驗證碼 + TTS 語音無障礙（可選）
- **自然人憑證** — MOICA 數位簽章認證 + OCSP/CRL 撤銷檢查（可選）

---

## 核心 API

### REST 端點

| Controller | 路徑前綴 | 主要操作 |
|-----------|----------|---------|
| `AuthController` | `/api/auth` | 登入、登出、刷新 Token、修改密碼 |
| `UserController` | `/api/users` | 使用者 CRUD、鎖定/解鎖、重設密碼 |
| `RoleController` | `/api/roles` | 角色管理、權限矩陣 CRUD |
| `MenuController` | `/api/menus` | 選單樹查詢 |
| `OrganizeController` | `/api/orgs` | 組織樹查詢 |

### 可選模組端點

| 模組 | Controller | 路徑 | 說明 |
|------|-----------|------|------|
| LDAP | `LdapController` | `/api/auth/ldap/login` | LDAP 認證登入 |
| OTP | `OtpController` | `/api/auth/otp/*` | 綁定/驗證 TOTP |
| CAPTCHA | `CaptchaController` | `/api/auth/captcha` | 取得/驗證圖形驗證碼 |
| MOICA | `CitizenCertController` | `/api/auth/cert/*` | 自然人憑證挑戰/登入 |

### 權限控制範例

```java
// 使用 RBAC 權限矩陣
@PreAuthorize("hasPermission('SC900', 'READ')")
@GetMapping("/api/reports")
public List<Report> list() { ... }

@PreAuthorize("hasPermission('SC900', 'CREATE')")
@PostMapping("/api/reports")
public Report create(@RequestBody ReportRequest req) { ... }
```

---

## 配置

```yaml
care:
  security:
    enabled: true                              # 是否啟用（預設 true）
    jwt:
      access-token-ttl-minutes: 30             # Access Token 有效時間
      refresh-token-ttl-days: 7                # Refresh Token 有效天數
      keystore-path: ./keys/jwt-keys.json      # JWK 金鑰路徑
    login:
      max-attempts: 5                          # 最大失敗次數
      lock-duration-minutes: 30                # 鎖定時間
    cors:
      allowed-origins: http://localhost:3000    # CORS 允許來源
    password:                                   # 密碼政策
      min-length: 8
      require-uppercase: true
      require-lowercase: true
      require-digit: true
      require-special: false
      history-count: 3                          # 不可重複最近 N 次密碼
    web:
      public-endpoints:                         # 不需認證的路徑
        - /api/auth/login
        - /api/auth/refresh
        - /api/auth/captcha
        - /api/auth/captcha/audio/**

    # === 可選模組 ===
    ldap:
      enabled: false                           # 啟用 LDAP（預設關）
      url: ldap://localhost:389
      base-dn: dc=example,dc=com
      user-search-filter: (sAMAccountName={0})
    otp:
      enabled: false                           # 啟用 OTP（預設關）
      issuer: CareSecuritySystem
    captcha:
      enabled: false                           # 啟用 CAPTCHA（預設關）
      length: 4
      expire-seconds: 300
      audio-enabled: false                     # TTS 語音驗證碼
    citizen-cert:
      enabled: false                           # 啟用自然人憑證（預設關）
      ocsp-enabled: true
      crl-enabled: true
      crl-cache-ttl-hours: 1
      ocsp-connect-timeout-ms: 5000
      ocsp-read-timeout-ms: 10000
      crl-connect-timeout-ms: 5000
      crl-read-timeout-ms: 15000
```

---

## 設計決策

### 要什麼

| 決策 | 原因 |
|------|------|
| OAuth2 Authorization Server | 標準協定，支援跨服務認證 |
| RBAC 權限矩陣（Menu × CRUD） | 精細到每個功能頁面的 CRUD 控制 |
| 可插拔認證模組 | LDAP、OTP、CAPTCHA、MOICA 各自獨立，加依賴即啟用 |
| Redis Token 黑名單 | 登出後 Token 立即失效，不用等到期 |
| 密碼歷史 | 防止使用者循環使用同一組密碼 |
| SHA-512 + Salt | Password4j 實作，比 bcrypt 更快且安全足夠 |

### 不要什麼

| 決策 | 原因 |
|------|------|
| Session 認證 | RESTful API 使用 Stateless JWT |
| 前端程式碼 | 前後端分離，前端由消費端自行實作 |
| 內建 Email 發送 | 密碼重設通知由消費端整合 |
| 多租戶 | 目前為單租戶設計，避免過度複雜 |

---

## 依賴關係

```
common-security-spring-boot-starter
└── common-security-autoconfigure
    └── common-security-core
        ├── common-log-spring-boot-starter        ← 日誌
        ├── common-response-spring-boot-starter   ← 統一回應
        └── common-jpa-spring-boot-starter        ← Entity 審計

可選模組（各自獨立，加依賴即啟用）：
├── common-security-auth-ldap       ← LDAP 認證
├── common-security-auth-otp        ← TOTP 兩步驟驗證
├── common-security-auth-captcha    ← 圖形驗證碼
└── common-security-auth-moica      ← 自然人憑證
    └── common-security-cert-core   ← 憑證驗證共用工具
```

---

## 專案結構與技術規格

```
care-security/
├── common-security-core/              # 核心：Entity + Service + Controller + Security
│   ├── config/                        框架配置（SecurityConfig, CORS, OpenAPI...）
│   ├── controller/                    REST API
│   ├── dto/request/ + response/       DTO
│   ├── entity/                        JPA Entity（SaUser, Role, Perm, Menu...）
│   ├── repository/                    Spring Data JPA
│   ├── security/                      認證授權（UserDetails, JWT, RBAC, Redis 黑名單）
│   └── service/                       業務邏輯
├── common-security-cert-core/         # 憑證驗證共用（OCSP + CRL）
├── common-security-auth-ldap/         # LDAP 認證
├── common-security-auth-otp/          # TOTP 兩步驟驗證
├── common-security-auth-captcha/      # 圖形驗證碼 + TTS
├── common-security-auth-moica/        # 自然人憑證
├── common-security-autoconfigure/     # AutoConfiguration
├── common-security-spring-boot-starter/ # Starter 空殼
└── common-security-test/              # 207 個整合測試（9 Phase）
```

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| Spring Security | 7.x |
| OAuth2 | Spring Authorization Server |
| 資料庫 | MSSQL (SQL Server) |
| 快取 | Redis |
| 密碼加密 | Password4j (SHA-512) |
| API 文件 | SpringDoc OpenAPI 3 |
| 自動配置 | `AutoConfiguration.imports` |

---

## 版本

- 1.0.0 — 初始版本：JWT 認證、RBAC、使用者管理、LDAP、OTP、CAPTCHA、自然人憑證、密碼政策、稽核日誌
