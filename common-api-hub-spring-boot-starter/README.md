# common-api-hub-spring-boot-starter

統一 API 認證閘道 — 外部系統帳密驗證 + JWT Token 簽發 + IP 白名單 + 呼叫日誌，引入即用。

---

## 這個模組在做什麼

讓你的系統可以**安全地開放 API 給外部系統呼叫**。

```
外部系統 A ──→ POST /api/hub/token（帳密 + URI）──→ 拿到 JWT
外部系統 A ──→ GET /api/xxx（帶 X-Hub-Token）──→ HubFilter 驗證 ──→ 放行
外部系統 B ──→ 沒有權限 ──→ 401
```

---

## 加入後你的專案自動獲得

| 功能 | 加入前 | 加入後 |
|------|--------|--------|
| 外部 API 認證 | 自己寫 Filter + JWT | 設定即用，帳密/Token 雙模式 |
| IP 白名單 | 沒有 | 單一 IP / CIDR / 範圍，per 使用者設定 |
| API 權限控制 | 自己管理 | 哪個使用者能打哪些 URI，有效期限控制 |
| 呼叫日誌 | 自己記 | 自動記錄每次呼叫（IP、參數脫敏、結果、耗時） |
| Token 管理 | 自己簽發 | JWT 簽發 + 續期 + 黑名單 |
| 密碼安全 | 明文 | BCrypt 加密 |

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-api-hub-spring-boot-starter</artifactId>
</dependency>
```

### 2. 設定 application.yml

```yaml
common:
  api-hub:
    enabled: true
    jwt:
      secret-key: your-secret-key-at-least-32-chars
    ip-whitelist:
      allow-local: true
    log:
      mask-fields:
        - password
        - passcode
        - secret
      retention-days: 90
```

### 3. 透過管理 API 設定

```bash
# 建立 API 設定
POST /api/hub/admin/api-sets
{ "name": "使用者查詢", "uri": "/api/users/**", "jwtTokenAging": 3600, "enabled": true }

# 建立介接使用者（密碼自動 BCrypt）
POST /api/hub/admin/users
{ "username": "system-a", "password": "xxx", "verifyIp": "192.168.1.0/24", "enabled": true }

# 授權使用者存取 API
POST /api/hub/admin/user-sets
{ "hubSet": {"id": 1}, "hubUser": {"id": 1},
  "verifyDts": "2026-01-01", "verifyDte": "2026-12-31",
  "userVerify": true, "jwtTokenVerify": true, "enabled": true }
```

### 4. 外部系統呼叫

```bash
# Step 1: 帳密換 Token
curl -X POST http://your-host/api/hub/token \
  -H "Content-Type: application/json" \
  -d '{"username":"system-a","password":"xxx","uri":"/api/users/**"}'

# 回應
{ "code": "200002", "message": "Token 簽發成功", "data": { "token": "eyJ...", "expiresIn": 3600 } }

# Step 2: 帶 Token 呼叫 API
curl http://your-host/api/users/123 \
  -H "X-Hub-Token: eyJ..."

# Step 3: Token 續期
curl -X POST http://your-host/api/hub/token/refresh \
  -H "X-Hub-Token: eyJ..."
```

---

## 功能總覽

### 四層認證架構

```
請求進入（帶 X-Hub-Token header）
  │
  ├── Layer 1: URI 匹配（AntPathMatcher，支援 /api/users/**）
  │     └─ 401001: URI 不在管控範圍
  │
  ├── Layer 2: Token 認證（JWT HS256 驗證）
  │     ├─ 422001: Token 無效（被竄改/黑名單）
  │     └─ 422002: Token 已過期
  │
  ├── Layer 3: 授權檢查（HubUserSet）
  │     ├─ 啟用檢查（HubSet + HubUser + HubUserSet 三層）
  │     ├─ 有效期（verifyDts ≤ today ≤ verifyDte）
  │     ├─ 認證策略（userVerify / jwtTokenVerify）
  │     └─ 401002: 授權不足
  │
  └── Layer 4: IP 白名單（Apache Commons Net CIDR）
        └─ 401003: IP 不在白名單
```

### IP 白名單格式

| 格式 | 範例 | 說明 |
|------|------|------|
| 單一 IP | `192.168.1.10` | 精確匹配 |
| CIDR | `192.168.1.0/24` | 子網路遮罩 |
| 範圍 | `192.168.1.10-20` | 最後一段範圍 |
| 多行 | 換行分隔 | 組合使用 |

### 認證策略（per 授權設定）

| userVerify | jwtTokenVerify | 允許 |
|------------|----------------|------|
| true | false | 只能帳密 |
| false | true | 只能 Token |
| true | true | 都可以 |

### 日誌脫敏

請求參數自動脫敏：
```
原始：{"username":"admin","password":"mySecret123"}
儲存：{"username":"admin","password":"***"}
```

脫敏欄位可配置（預設：password、passcode、secret）。

---

## API 端點

### 外部系統（公開）

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/hub/token` | POST | 帳密 + URI → JWT Token |
| `/api/hub/token/refresh` | POST | Token 續期（X-Hub-Token header） |

### 管理（需權限）

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/hub/admin/api-sets` | GET / POST / PUT / DELETE | API 設定 CRUD |
| `/api/hub/admin/users` | GET / POST / PUT / DELETE | 介接使用者 CRUD |
| `/api/hub/admin/user-sets` | GET / POST / PUT / DELETE | 授權設定 CRUD |
| `/api/hub/admin/logs` | GET | 分頁查詢呼叫日誌 |

---

## 回應格式

```json
// 成功
{ "code": "200002", "message": "Token 簽發成功", "data": { "token": "eyJ...", "expiresIn": 3600 } }

// 失敗
{ "code": "401002", "message": "認證失敗：密碼錯誤" }
```

### 回應代碼

| 代碼 | 常數 | 說明 |
|------|------|------|
| 200001 | SUCCESS | 操作成功 |
| 200002 | TOKEN_ISSUED | Token 簽發成功 |
| 200003 | TOKEN_REFRESHED | Token 續期成功 |
| 401001 | API_SET_DISABLED | URI 不在管控範圍 / API 停用 |
| 401002 | AUTH_FAILED | 帳號/密碼/授權錯誤 |
| 401003 | IP_DENIED | IP 不在白名單 |
| 422001 | TOKEN_INVALID | Token 無效（竄改/黑名單） |
| 422002 | TOKEN_EXPIRED | Token 已過期 |
| 500001 | INTERNAL_ERROR | 內部錯誤 |

---

## 配置項

| 配置 | 預設值 | 說明 |
|------|--------|------|
| `common.api-hub.enabled` | `false` | 全局開關 |
| `common.api-hub.jwt.secret-key` | `default-...` | JWT 密鑰（建議 32 字元以上） |
| `common.api-hub.ip-whitelist.allow-local` | `true` | 允許 localhost |
| `common.api-hub.log.mask-fields` | `[password, passcode, secret]` | 日誌脫敏欄位 |
| `common.api-hub.log.retention-days` | `90` | 日誌保留天數 |

---

## Domain 模型

| Entity | 表名 | 說明 |
|--------|------|------|
| HubSet | HUB_SET | API 設定（name, uri, jwtTokenAging, enabled） |
| HubUser | HUB_USER | 介接使用者（username, password BCrypt, verifyIp, enabled） |
| HubUserSet | HUB_USER_SET | 授權樞紐（hubSet + hubUser + 有效期 + 認證策略） |
| HubLog | HUB_LOG | 呼叫日誌（method, uri, params, ip, success, elapsedMs） |

---

## 專案結構

```
com.company.common.hub/
├── config/
│   ├── ApiHubAutoConfiguration.java       # @ConditionalOnProperty 自動配置
│   └── ApiHubProperties.java              # 配置屬性
├── controller/
│   ├── HubTokenController.java            # POST /api/hub/token + refresh
│   └── HubAdminController.java            # CRUD 管理端點
├── dto/
│   ├── HubAuthResult.java                 # 認證結果
│   ├── HubResponse.java                   # 統一回應封裝
│   ├── HubResponseCode.java              # 回應代碼常數
│   ├── TokenRequest.java                  # 登入請求（username, password, uri）
│   └── TokenResponse.java                # Token 回應（token, expiresIn）
├── entity/
│   ├── HubSet.java                        # extends AuditableEntity
│   ├── HubUser.java                       # extends AuditableEntity
│   ├── HubUserSet.java                    # extends AuditableEntity
│   └── HubLog.java                        # 獨立（createdAt only）
├── exception/
│   ├── HubAuthException.java             # 認證失敗（code + message）
│   └── HubTokenException.java            # Token 錯誤（code + message）
├── filter/
│   └── HubAuthenticationFilter.java       # OncePerRequestFilter
├── repository/
│   ├── HubSetRepository.java
│   ├── HubUserRepository.java
│   ├── HubUserSetRepository.java
│   └── HubLogRepository.java
└── service/
    ├── HubAuthService.java                # 4 層認證核心
    ├── HubTokenService.java               # JWT 簽發/驗證/黑名單
    ├── IpWhitelistService.java            # IP 比對（CIDR）
    └── HubLogService.java                 # 日誌記錄 + 脫敏
```

---

## 測試覆蓋

| Phase | 範圍 | 測試數 |
|-------|------|--------|
| 1 | Entity + Repository | 28 |
| 2 | IpWhitelistService | 19 |
| 3 | HubTokenService | 9 |
| 4 | HubLogService | 11 |
| 5 | HubAuthService（4 層認證） | 19 |
| 6 | HubAuthenticationFilter | 7 |
| 7 | Controller | 11 |
| 8 | AutoConfiguration | 5 |
| 9 | E2E 整合測試 | 6 |
| **合計** | | **118** |

---

## 依賴關係

```
common-api-hub-spring-boot-starter
├── common-jpa-spring-boot-starter        ← AuditableEntity
├── spring-boot-starter-web               ← Controller + Filter
├── spring-boot-starter-data-jpa          ← Repository
├── spring-security-crypto                ← BCryptPasswordEncoder
├── jjwt-api + jjwt-impl + jjwt-jackson  ← JWT（0.12.6）
└── commons-net                           ← CIDR IP 比對（3.11.1）
```

---

## 版本

- 1.0.0 — 初始版本：4 層認證、JWT 簽發/續期/黑名單、IP 白名單（CIDR）、呼叫日誌（脫敏）、管理 CRUD API、118 個 TDD 測試
