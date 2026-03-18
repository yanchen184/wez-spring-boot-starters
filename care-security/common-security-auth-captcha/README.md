# common-security-auth-captcha

CAPTCHA 圖形驗證碼模組 — 圖片驗證碼 + TTS 語音無障礙，登入時防機器人。

---

## 功能總覽

- **圖形驗證碼** — 隨機字元 + 干擾線 + 噪點，PNG Base64 輸出
- **語音驗證碼** — WAV 音訊 Base64（無障礙輔助），預錄中文數字 + 英文字母
- **Redis 一次性儲存** — 驗證碼存 Redis，驗證後立即刪除，防重放攻擊
- **SPI 整合** — 實作 `CaptchaVerifier` 介面，core 模組透過 SPI 呼叫
- **可配置** — 長度、字元集、圖片尺寸、過期時間、是否包含字母

---

## 啟用方式

```yaml
care:
  security:
    captcha:
      enabled: true     # 必須明確啟用
```

啟用後自動註冊 `CaptchaService`、`CaptchaController`。

---

## REST API

| 方法 | 路徑 | 說明 | 權限 |
|------|------|------|------|
| GET | `/api/auth/captcha` | 產生驗證碼圖片 | 公開 |
| GET | `/api/auth/captcha/audio/{captchaId}` | 取得語音驗證碼 | 公開 |

### 取得驗證碼

```
GET /api/auth/captcha

Response:
{
  "captchaId": "a1b2c3d4-...",
  "imageBase64": "iVBORw0KGgo..."
}
```

### 取得語音驗證碼

```
GET /api/auth/captcha/audio/{captchaId}

Response:
{
  "code": "OK",
  "data": {
    "audioBase64": "UklGR..."
  }
}
```

> 需要 `care.security.captcha.audio-enabled=true` 才會啟用。

---

## 核心類別

### CaptchaService

實作 `CaptchaVerifier` SPI 介面。

| 方法 | 說明 |
|------|------|
| `generateCaptcha()` | 產生驗證碼 → 存 Redis → 回傳 `CaptchaResult(captchaId, imageBase64)` |
| `verifyCaptcha(captchaId, answer)` | 驗證答案（不分大小寫），一次性使用，驗證後立即刪除 |
| `generateAudioBase64(captchaId)` | 將驗證碼轉為 WAV 音訊 Base64（拼接預錄語音） |

### CaptchaController

- `GET /api/auth/captcha` → `generateCaptcha()`
- `GET /api/auth/captcha/audio/{captchaId}` → `generateAudioBase64()`

### FormantSpeechSynthesizer

語音合成輔助工具。

---

## 前端整合範例

### 1. 取得驗證碼圖片

```javascript
const res = await fetch('/api/auth/captcha');
const { captchaId, imageBase64 } = await res.json();

// 顯示圖片
document.getElementById('captchaImg').src = `data:image/png;base64,${imageBase64}`;
```

### 2. 登入時帶入驗證碼

```javascript
const loginRes = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin',
    password: 'MyP@ssw0rd!',
    captchaId: captchaId,       // 驗證碼 ID
    captchaAnswer: '1234'       // 使用者輸入的驗證碼
  })
});
```

### 3. 語音驗證碼（無障礙）

```javascript
const audioRes = await fetch(`/api/auth/captcha/audio/${captchaId}`);
const { data } = await audioRes.json();

const audio = new Audio(`data:audio/wav;base64,${data.audioBase64}`);
audio.play();
```

---

## 配置項

前綴：`care.security.captcha`

```yaml
care:
  security:
    captcha:
      enabled: false              # 是否啟用（預設 false）
      length: 4                   # 驗證碼長度（預設 4）
      chars: null                 # 自訂字元集（null = 自動決定）
      include-letters: false      # 包含英文字母（預設 false = 純數字）
                                  # true → "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ"
                                  # false → "0123456789"
                                  # 排除了易混淆的 I 和 O
      width: 160                  # 圖片寬度 px（預設 160）
      height: 50                  # 圖片高度 px（預設 50）
      font-size: 32               # 字型大小 px（預設 32）
      audio-enabled: false        # 啟用語音驗證碼（預設 false）
      expire-seconds: 300         # 過期秒數（預設 300 = 5 分鐘）
```

---

## SPI 介面

`CaptchaVerifier` 定義在 core 模組：

```java
public interface CaptchaVerifier {
    boolean verifyCaptcha(String captchaId, String answer);
}
```

`CaptchaService` 實作此介面，`AuthService` 透過 `ObjectProvider<CaptchaVerifier>` 可選注入。

未啟用 CAPTCHA 時，`AuthService` 中的 `captchaVerifier` 為 `null`，登入流程跳過驗證碼檢查。

---

## Redis 儲存

| Key 格式 | Value | TTL |
|----------|-------|-----|
| `captcha:{captchaId}` | 驗證碼答案（如 `1234`） | `expireSeconds`（預設 300 秒） |

---

## 技術規格

| 項目 | 值 |
|------|-----|
| 圖片格式 | PNG（Java AWT 繪製） |
| 音訊格式 | WAV 16kHz 16-bit mono |
| 儲存 | Redis（TTL 自動過期） |
| 安全 | 一次性使用、不分大小寫比對 |
