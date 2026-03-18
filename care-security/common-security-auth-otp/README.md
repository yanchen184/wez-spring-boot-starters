# common-security-auth-otp

TOTP 兩步驟驗證模組 — RFC 6238 實作，相容 Google Authenticator / Microsoft Authenticator。

---

## 功能總覽

- **TOTP 實作** — RFC 6238，6 位數字，30 秒一組，HmacSHA1
- **QR Code URI** — 產生 `otpauth://` URI，掃碼即可加入 App
- **完整生命週期** — 設定 → 驗證啟用 → 登入驗證 → 停用
- **時間容錯** — 支援 `allowedSkew`（預設 +/- 30 秒）
- **SPI 整合** — 實作 `OtpChecker` 介面
- **暴力破解防護** — 搭配 `LoginAttemptService` 計數失敗次數

---

## 啟用方式

```yaml
care:
  security:
    otp:
      enabled: true                    # 必須明確啟用
      issuer: MyCompanySystem          # 顯示在 App 上的發行者名稱
```

啟用後自動註冊 `TotpService`、`OtpService`、`OtpController`。

---

## 完整流程

### 第一次設定 OTP

```
1. [已登入] POST /api/auth/otp/setup
   → { "secret": "JBSWY3DPEHPK3PXP", "otpAuthUri": "otpauth://totp/..." }

2. 前端用 otpAuthUri 產生 QR Code

3. 使用者用 Google Authenticator 掃描 QR Code

4. [已登入] POST /api/auth/otp/verify-setup
   Body: { "code": "123456" }
   → OTP 啟用成功
```

### 登入時 OTP 驗證

```
1. POST /api/auth/login
   Body: { "username": "admin", "password": "MyP@ssw0rd!" }
   → { "requiresOtp": true }    ← 帳密正確但需要 OTP

2. POST /api/auth/otp/verify
   Body: { "username": "admin", "code": "654321" }
   → { "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

### 停用 OTP

```
[已登入] DELETE /api/auth/otp
→ OTP 已停用，下次登入不再需要驗證碼
```

---

## REST API

| 方法 | 路徑 | 說明 | 權限 |
|------|------|------|------|
| POST | `/api/auth/otp/setup` | 產生 OTP secret + QR URI | 已認證 |
| POST | `/api/auth/otp/verify-setup` | 驗證啟用 OTP | 已認證 |
| POST | `/api/auth/otp/verify` | 登入時 OTP 驗證（回傳 JWT） | 公開 |
| DELETE | `/api/auth/otp` | 停用 OTP | 已認證 |

---

## 核心類別

### OtpService

實作 `OtpChecker` SPI 介面。管理 OTP 的生命週期。

| 方法 | 說明 |
|------|------|
| `setupOtp(username)` | 產生新 secret，存入 DB（otpEnabled=false），回傳 `OtpSetupResult` |
| `verifyAndEnableOtp(username, code)` | 驗證 App 回傳的 code，成功後 otpEnabled=true |
| `verifyOtp(username, code)` | 登入時驗證 OTP code |
| `isOtpEnabled(username)` | 檢查使用者是否啟用 OTP（SPI 方法） |
| `disableOtp(username)` | 停用 OTP，清除 secret |

### TotpService

底層 TOTP 演算法實作，不涉及任何 DB 操作。

| 方法 | 說明 |
|------|------|
| `generateSecret()` | 產生 20 bytes 隨機 Base32 secret |
| `generateCode(secret)` | 產生當前時間的 6 位 OTP code |
| `verifyCode(secret, code)` | 驗證 code（含時間容錯） |
| `buildOtpAuthUri(secret, username, issuer)` | 產生 `otpauth://totp/...` URI |

**安全特性：**
- `SecureRandom` 產生 secret
- `constantTimeEquals` 防止 timing attack
- 內建 Base32 編解碼（RFC 4648）

---

## 前端整合範例

### 設定 OTP

```javascript
// 1. 呼叫 setup
const res = await fetch('/api/auth/otp/setup', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${accessToken}` }
});
const { secret, otpAuthUri } = await res.json();

// 2. 用 otpAuthUri 產生 QR Code（使用 qrcode.js 等套件）
QRCode.toCanvas(document.getElementById('qrCanvas'), otpAuthUri);

// 3. 使用者掃碼後，輸入 App 顯示的驗證碼
const verifyRes = await fetch('/api/auth/otp/verify-setup', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ code: '123456' })
});
```

### 登入 OTP 驗證

```javascript
// 第一步登入回傳 requiresOtp: true
const loginRes = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'MyP@ssw0rd!' })
});
const loginData = await loginRes.json();

if (loginData.requiresOtp) {
  // 顯示 OTP 輸入框，使用者輸入後送出
  const otpRes = await fetch('/api/auth/otp/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', code: userInputCode })
  });
  const { data } = await otpRes.json();
  // data.accessToken, data.refreshToken
}
```

---

## 配置項

前綴：`care.security.otp`

```yaml
care:
  security:
    otp:
      enabled: false                  # 是否啟用（預設 false）
      issuer: CareSecuritySystem      # 發行者名稱（預設 CareSecuritySystem）
                                      # 顯示在 Google Authenticator App 上
      allowed-skew: 1                 # 時間偏移容錯步數（預設 1）
                                      # 1 = 允許前後各 1 個 30 秒視窗
                                      # 2 = 允許前後各 2 個 30 秒視窗（較寬鬆）
```

---

## SPI 介面

`OtpChecker` 定義在 core 模組：

```java
public interface OtpChecker {
    boolean isOtpEnabled(String username);
}
```

`AuthService` 透過 `ObjectProvider<OtpChecker>` 可選注入。
登入時，若 `otpChecker != null && otpChecker.isOtpEnabled(username)` 為 true，
回傳 `requiresOtp=true`，要求前端進行第二步驗證。

---

## 資料儲存

OTP secret 儲存在 `SaUser` entity 中：

| 欄位 | 型態 | 說明 |
|------|------|------|
| `otpSecret` | `String` | Base32 編碼的 TOTP secret |
| `otpEnabled` | `Boolean` | 是否已啟用 OTP |

---

## 技術規格

| 項目 | 值 |
|------|-----|
| 演算法 | TOTP (RFC 6238)，HmacSHA1 |
| Code 長度 | 6 位數字 |
| 時間步長 | 30 秒 |
| Secret 長度 | 20 bytes (160 bits) |
| 編碼 | Base32 (RFC 4648) |
| 相容 App | Google Authenticator / Microsoft Authenticator / Authy |
