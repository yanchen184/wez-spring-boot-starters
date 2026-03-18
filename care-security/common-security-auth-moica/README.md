# common-security-auth-moica

台灣自然人憑證（MOICA）數位簽章認證模組 — 挑戰碼 + PKCS#7 簽章驗證 + 自動建帳。

---

## 功能總覽

- **挑戰-回應認證** — Server 產生隨機挑戰碼，Client 用自然人憑證簽章
- **PKCS#7 驗證** — 驗證數位簽章的完整性和真實性
- **憑證鏈驗證** — MOICA 中繼 CA 憑證驗證
- **撤銷檢查** — OCSP + CRL 雙重檢查（可個別關閉）
- **自動建帳** — 憑證驗證成功後，自動建立/同步本地 SaUser
- **一次性 Token** — 挑戰碼 Redis 儲存，使用後立即銷毀

---

## 啟用方式

```yaml
care:
  security:
    citizen-cert:
      enabled: true
      intermediate-cert-paths:
        - classpath:moica/MOICA2.cer
        - classpath:moica/MOICA3.cer
```

啟用後自動註冊 `LoginTokenService`、`MoicaCertService`、`CitizenCertUserSyncService`、`CitizenCertController`。

---

## 完整認證流程

```
         瀏覽器                              伺服器
           │                                  │
           │  1. GET /api/auth/cert/login-token │
           │ ──────────────────────────────────>│
           │                                  │  產生隨機 loginToken
           │                                  │  存入 Redis（TTL 300s）
           │  { "loginToken": "abc123..." }   │
           │ <────────────────────────────────│
           │                                  │
           │  2. 用自然人憑證對 loginToken       │
           │     進行 PKCS#7 簽章               │
           │     （透過瀏覽器插件 / HiPKI）       │
           │                                  │
           │  3. POST /api/auth/cert/login     │
           │  { "loginToken": "abc123...",     │
           │    "base64Data": "MIIBxjCC..." }  │
           │ ──────────────────────────────────>│
           │                                  │  a. 驗證 loginToken（一次性）
           │                                  │  b. 解析 PKCS#7 信封
           │                                  │  c. 驗證數位簽章
           │                                  │  d. 驗證憑證有效期
           │                                  │  e. 驗證中繼 CA 憑證鏈
           │                                  │  f. OCSP/CRL 撤銷檢查
           │                                  │  g. 提取身分資訊（CN、身分證後 4 碼）
           │                                  │  h. 同步/建立使用者
           │                                  │  i. 產生 JWT tokens
           │                                  │
           │  { "accessToken": "eyJ...",       │
           │    "refreshToken": "eyJ..." }     │
           │ <────────────────────────────────│
```

---

## REST API

| 方法 | 路徑 | 說明 | 權限 |
|------|------|------|------|
| GET | `/api/auth/cert/login-token` | 取得登入挑戰碼 | 公開 |
| POST | `/api/auth/cert/login` | 憑證登入（PKCS#7 簽章驗證） | 公開 |

### 取得挑戰碼

```
GET /api/auth/cert/login-token

Response:
{
  "loginToken": "a1b2c3d4-e5f6-7890-..."
}
```

### 憑證登入

```
POST /api/auth/cert/login
Content-Type: application/json

{
  "loginToken": "a1b2c3d4-e5f6-7890-...",
  "base64Data": "MIIBxjCCAW2gAwIB..."
}

Response:
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiJ9..."
}
```

---

## 核心類別

### CitizenCertController

REST 端點，串接整個認證流程。

### LoginTokenService

挑戰碼管理，Redis 儲存。

| 方法 | 說明 |
|------|------|
| `generateLoginToken()` | 產生 UUID 隨機挑戰碼，存 Redis |
| `consumeLoginToken(token)` | 驗證並銷毀（一次性使用，防重放） |

### MoicaCertService

憑證驗證服務。

| 方法 | 說明 |
|------|------|
| `fullVerify(certificate)` | 完整驗證（有效期 + 憑證鏈 + 撤銷檢查） |
| `createVerifier(certificate)` | 建立 `MoicaCertUtils` 實例 |

**驗證項目：**

| 步驟 | 說明 | 失敗例外 |
|------|------|---------|
| 有效期 | `checkValidity()` 確認憑證在有效期內 | `MoicaExpiredException` / `MoicaNotYetValidException` |
| 憑證鏈 | 驗證發行者是否為信任的 MOICA 中繼 CA | `MoicaLoginException` |
| 撤銷 | OCSP + CRL 檢查憑證是否已被撤銷 | `MoicaRevocationException` |

### MoicaCertUtils

X.509 憑證工具。

| 方法 | 說明 |
|------|------|
| `getSubjectCName()` | 取得 CN（姓名） |
| `getExtLast4IDNO()` | 取得身分證字號後 4 碼 |
| `getSN()` | 取得憑證序號 |
| `validIntermediateCert()` | 驗證中繼 CA 憑證鏈 |
| `isRevoked()` | OCSP/CRL 撤銷檢查 |
| `checkValidity()` | 有效期檢查 |

### Pkcs7Utils

PKCS#7 信封工具。

| 方法 | 說明 |
|------|------|
| `new Pkcs7Utils(base64Data, loginToken)` | 解析 PKCS#7 信封 |
| `valid()` | 驗證數位簽章 |
| `getCert()` | 取得簽章者 X.509 憑證 |
| `getCardSN()` | 取得晶片卡序號 |

### CitizenCertUserSyncService

使用者同步：依 CN + 身分證後 4 碼查找/建立使用者。

---

## 配置項

前綴：`care.security.citizen-cert`

```yaml
care:
  security:
    citizen-cert:
      enabled: false                        # 是否啟用（預設 false）
      challenge-expire-seconds: 300         # 挑戰碼過期秒數（預設 300）
      auto-create-user: true                # 自動建立使用者帳號（預設 true）
      default-roles:                        # 憑證使用者預設角色
        - ROLE_USER
      intermediate-cert-paths:              # MOICA 中繼 CA 憑證路徑
        - classpath:moica/MOICA2.cer        # MOICA 第二代中繼 CA
        - classpath:moica/MOICA3.cer        # MOICA 第三代中繼 CA
      local-crl-paths:                      # 本地 CRL 路徑（離線撤銷檢查）
        - classpath:moica/MOICA2.crl
        - classpath:moica/MOICA3.crl
      ocsp-enabled: true                    # 啟用 OCSP 線上撤銷檢查（預設 true）
      crl-enabled: true                     # 啟用 CRL 撤銷檢查（預設 true）
      crl-cache-ttl-hours: 1                # CRL 快取時間（小時，預設 1）
```

### 中繼憑證設定

MOICA 自然人憑證需要中繼 CA 憑證來驗證憑證鏈。將 `.cer` 檔案放在 `src/main/resources/moica/` 下：

```
src/main/resources/
└── moica/
    ├── MOICA2.cer      # 第二代中繼 CA 憑證
    ├── MOICA3.cer      # 第三代中繼 CA 憑證
    ├── MOICA2.crl      # 第二代 CRL（可選）
    └── MOICA3.crl      # 第三代 CRL（可選）
```

> 中繼 CA 憑證可從 MOICA 官網（https://moica.nat.gov.tw）下載。

---

## 例外類別

| 例外 | 父類 | 說明 |
|------|------|------|
| `MoicaLoginException` | `BadCredentialsException` | 一般認證失敗 |
| `MoicaExpiredException` | `MoicaLoginException` | 憑證已過期 |
| `MoicaNotYetValidException` | `MoicaLoginException` | 憑證尚未生效 |
| `MoicaRevocationException` | `MoicaLoginException` | 憑證已被撤銷 |
| `MoicaUserNotFoundException` | `MoicaLoginException` | 使用者不存在且未開啟自動建帳 |

---

## Redis 儲存

| Key 格式 | Value | TTL |
|----------|-------|-----|
| `cert:login-token:{token}` | `"1"` | `challengeExpireSeconds`（預設 300 秒） |

---

## 技術規格

| 項目 | 值 |
|------|-----|
| 簽章格式 | PKCS#7（CMS SignedData） |
| 憑證格式 | X.509 v3 |
| 撤銷檢查 | OCSP + CRL |
| CRL 快取 | 記憶體快取，TTL 可配 |
| 挑戰碼 | UUID + Redis 一次性使用 |
