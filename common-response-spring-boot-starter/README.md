# Common Response Spring Boot Starter

統一回應模組 — API Response 封裝、全局異常處理、錯誤碼管理、分頁回應

---

## 目錄

- [加入後你的專案自動獲得](#加入後你的專案自動獲得)
- [快速開始](#快速開始)
- [功能總覽](#功能總覽)
- [核心 API](#核心-api)
- [配置](#配置)
- [設計決策](#設計決策)
- [依賴關係](#依賴關係)
- [專案結構與技術規格](#專案結構與技術規格)
- [版本](#版本)

---

## 加入後你的專案自動獲得

| 功能 | 加入前 | 加入後 |
|------|--------|--------|
| API 回應格式 | 每個 Controller 自己包 `{success, data}` | 所有 API 自動包裝為 `ApiResponse<T>` |
| 異常處理 | 每個 Controller 自己 try-catch | 12 種異常自動轉換為標準錯誤回應 |
| 錯誤碼 | 散落各處的字串常數 | 預定義 A/B/C/D 分類錯誤碼體系 |
| 驗證錯誤 | 自己解析 `BindingResult` | 自動包含 field + message + rejectedValue |
| 分頁回應 | 自己組裝分頁 JSON | `PageResponse.of()` 一行搞定 |
| 配置 | — | 零配置，引入依賴即生效 |

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-response-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 寫一個 Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        // 直接回傳業務物件，自動包裝成 ApiResponse
        return userService.findById(id);
    }

    @PostMapping
    public void createUser(@Valid @RequestBody CreateUserRequest request) {
        userService.create(request);
        // void 也會自動包裝
    }
}
```

### 3. 完成

不需要任何配置。回應自動包裝、異常自動處理。

---

## 功能總覽

- **統一回應格式** — 所有 API 自動包裝為 `ApiResponse<T>` 格式
- **全局異常處理** — `BusinessException`、驗證錯誤、404、500 等自動轉換為標準錯誤回應
- **錯誤碼體系** — 預定義 `CommonErrorCode`、`UserErrorCode`、`OrderErrorCode`、`FileErrorCode`，可擴展
- **分頁回應** — `PageResponse<T>` 統一分頁格式
- **路徑排除** — 指定路徑（如 `/actuator/**`）不走統一包裝與異常處理
- **零配置** — 引入依賴即自動生效

---

## 核心 API

### ApiResponse — 統一回應封裝

| 方法 | 回傳 | 說明 |
|------|------|------|
| `ApiResponse.ok()` | `ApiResponse<T>` | 成功，無資料 |
| `ApiResponse.ok(data)` | `ApiResponse<T>` | 成功，帶資料 |
| `ApiResponse.ok(message, data)` | `ApiResponse<T>` | 成功，自訂訊息 |
| `ApiResponse.error(message)` | `ApiResponse<T>` | 失敗，用 `BAD_REQUEST` 錯誤碼 |
| `ApiResponse.error(errorCode)` | `ApiResponse<T>` | 失敗，指定錯誤碼 |
| `ApiResponse.error(errorCode, message)` | `ApiResponse<T>` | 失敗，指定錯誤碼 + 自訂訊息 |
| `ApiResponse.error(code, message)` | `ApiResponse<T>` | 失敗，自訂 code 與 message |
| `ApiResponse.validationError(errors)` | `ApiResponse<T>` | 驗證錯誤，帶欄位錯誤列表 |

#### 回應格式範例

**成功回應：**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "id": 1,
    "name": "Alice"
  }
}
```

**錯誤回應：**
```json
{
  "success": false,
  "code": "A0001",
  "message": "請求參數錯誤"
}
```

**驗證錯誤回應：**
```json
{
  "success": false,
  "code": "A0002",
  "message": "驗證失敗",
  "errors": [
    {
      "field": "email",
      "message": "不得為空",
      "rejectedValue": null
    },
    {
      "field": "name",
      "message": "長度需介於 2 到 50 之間",
      "rejectedValue": "A"
    }
  ]
}
```

**分頁回應：**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "content": [{"id": 1}, {"id": 2}],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### BusinessException — 業務異常

```java
// 使用預定義錯誤碼
throw new BusinessException(CommonErrorCode.INSUFFICIENT_BALANCE);

// 自訂訊息
throw new BusinessException(CommonErrorCode.BUSINESS_ERROR, "訂單已取消，無法修改");

// 自訂錯誤碼
throw new BusinessException("ORDER_001", "訂單不存在");

// 語意化靜態工廠
throw BusinessException.notFound("用戶不存在");
throw BusinessException.unauthorized("請先登入");
throw BusinessException.forbidden("無權限");
throw BusinessException.badRequest("參數錯誤");
throw BusinessException.conflict("資料重複");
throw BusinessException.internal("系統異常");
```

| 靜態工廠方法 | 對應錯誤碼 | HTTP Status |
|-------------|-----------|-------------|
| `notFound(msg)` | `A0300` | 404 |
| `unauthorized(msg)` | `A0100` | 401 |
| `forbidden(msg)` | `A0200` | 403 |
| `badRequest(msg)` | `A0001` | 400 |
| `conflict(msg)` | `A0500` | 409 |
| `internal(msg)` | `C0001` | 500 |

### PageResponse — 分頁回應

```java
// 從查詢結果建立
Page<UserDTO> page = userRepository.findAll(pageable);
PageResponse<UserDTO> pageResponse = PageResponse.of(
    page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements()
);

// 直接包裝成 ApiResponse
return pageResponse.toApiResponse();
```

搭配 Controller：

```java
@GetMapping
public ApiResponse<PageResponse<UserDTO>> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

    Page<UserDTO> result = userService.findAll(PageRequest.of(page, size));
    return PageResponse.of(
        result.getContent(), result.getNumber(),
        result.getSize(), result.getTotalElements()
    ).toApiResponse();
}
```

### 錯誤碼體系

#### 規則

| 前綴 | 分類 | 說明 |
|------|------|------|
| `SUCCESS` | 成功 | 操作成功 |
| `A0xxx` | 用戶端錯誤 | 參數、權限、資源不存在 |
| `B0xxx` | 業務錯誤 | 業務邏輯失敗 |
| `C0xxx` | 系統錯誤 | 內部異常、資料庫、快取 |
| `D0xxx` | 外部服務錯誤 | 第三方服務呼叫失敗 |

#### CommonErrorCode（通用）

| 錯誤碼 | 列舉值 | 訊息 | HTTP |
|--------|--------|------|------|
| `SUCCESS` | `SUCCESS` | 操作成功 | 200 |
| `A0001` | `BAD_REQUEST` | 請求參數錯誤 | 400 |
| `A0002` | `VALIDATION_ERROR` | 驗證失敗 | 400 |
| `A0003` | `INVALID_PARAMETER` | 無效的參數 | 400 |
| `A0004` | `MISSING_PARAMETER` | 缺少必要參數 | 400 |
| `A0100` | `UNAUTHORIZED` | 未授權，請先登入 | 401 |
| `A0101` | `INVALID_TOKEN` | 無效的 Token | 401 |
| `A0102` | `TOKEN_EXPIRED` | Token 已過期 | 401 |
| `A0200` | `FORBIDDEN` | 無權限執行此操作 | 403 |
| `A0300` | `NOT_FOUND` | 資源不存在 | 404 |
| `A0400` | `METHOD_NOT_ALLOWED` | 不支援的請求方法 | 405 |
| `A0500` | `CONFLICT` | 資源衝突 | 409 |
| `A0600` | `TOO_MANY_REQUESTS` | 請求過於頻繁 | 429 |
| `B0001` | `BUSINESS_ERROR` | 業務處理失敗 | 400 |
| `C0001` | `INTERNAL_ERROR` | 系統內部錯誤 | 500 |
| `D0001` | `EXTERNAL_SERVICE_ERROR` | 外部服務呼叫失敗 | 502 |

#### 業務錯誤碼

除了 `CommonErrorCode`，還內建三組業務錯誤碼，可直接使用：

- **`UserErrorCode`** — 用戶相關（登入註冊、密碼、帳號狀態、驗證碼）
- **`OrderErrorCode`** — 訂單相關（訂單狀態、支付、庫存、配送）
- **`FileErrorCode`** — 檔案相關（上傳、下載、儲存）

#### 自訂錯誤碼

實作 `ErrorCode` 介面，定義專案自己的錯誤碼：

```java
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_DECLINED("PAY_B001", "支付被拒絕", 400),
    CARD_EXPIRED("PAY_B002", "信用卡已過期", 400),
    REFUND_LIMIT_EXCEEDED("PAY_B003", "退款次數已達上限", 400);

    private final String code;
    private final String message;
    private final int httpStatus;

    PaymentErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public int getHttpStatus() { return httpStatus; }
}
```

搭配 `BusinessException` 使用：

```java
throw new BusinessException(PaymentErrorCode.PAYMENT_DECLINED);
throw new BusinessException(PaymentErrorCode.CARD_EXPIRED, "Visa 卡已於 2024/01 過期");
```

### 全局異常處理

`GlobalExceptionHandler` 自動處理以下異常：

| 異常 | 錯誤碼 | HTTP Status | 說明 |
|------|--------|-------------|------|
| `BusinessException` | 自訂 | 自訂 | 業務異常 |
| `MethodArgumentNotValidException` | `A0002` | 400 | `@Valid` 驗證失敗 |
| `BindException` | `A0002` | 400 | 綁定異常 |
| `MissingServletRequestParameterException` | `A0004` | 400 | 缺少請求參數 |
| `MethodArgumentTypeMismatchException` | `A0003` | 400 | 參數類型不匹配 |
| `HttpMessageNotReadableException` | `A0001` | 400 | 請求體格式錯誤 |
| `HttpRequestMethodNotSupportedException` | `A0400` | 405 | 不支援的 HTTP 方法 |
| `HttpMediaTypeNotSupportedException` | `A0001` | 415 | 不支援的 Content-Type |
| `NoHandlerFoundException` / `NoResourceFoundException` | `A0300` | 404 | 資源不存在 |
| `IllegalArgumentException` | `A0001` | 400 | 參數不合法 |
| `Exception`（其他） | `C0001` | 500 | 未知異常（不暴露詳細訊息） |

> 異常處理器使用 `@Order(Ordered.LOWEST_PRECEDENCE)`，安全相關異常可由各 starter 用更高優先級攔截。

### 進階用法

#### 覆蓋 GlobalExceptionHandler

定義自己的 Bean 即可取代預設的異常處理器（`@ConditionalOnMissingBean`）：

```java
@Bean
public GlobalExceptionHandler globalExceptionHandler(ResponseProperties properties) {
    return new MyCustomExceptionHandler(properties);
}
```

#### 覆蓋 GlobalResponseAdvice

同理，定義自己的 Bean 即可取代預設的回應包裝：

```java
@Bean
public GlobalResponseAdvice globalResponseAdvice(ResponseProperties properties, JsonMapper jsonMapper) {
    return new MyCustomResponseAdvice(properties, jsonMapper);
}
```

#### 停用自動包裝

如果只需要異常處理，不需要自動包裝回應：

```java
// Controller 回傳 ApiResponse 就不會被重複包裝
@GetMapping("/{id}")
public ApiResponse<UserDTO> getUser(@PathVariable Long id) {
    return ApiResponse.ok(userService.findById(id));
}
```

`GlobalResponseAdvice` 偵測到回傳類型已經是 `ApiResponse` 時，會跳過包裝。

#### 搭配 Spring Security

安全相關的 starter（如 care-security）可以定義更高優先級的 `@RestControllerAdvice` 攔截 `AuthenticationException`、`AccessDeniedException` 等，不會與本模組衝突：

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(401)
            .body(ApiResponse.error(CommonErrorCode.UNAUTHORIZED));
    }
}
```

#### 搭配 i18n 錯誤訊息

Starter 不內建 i18n，但消費端可以透過 `MessageSource` 輕鬆實現：

**1. 建立翻譯檔**

```properties
# src/main/resources/messages.properties（預設 / 中文）
SC_B001=商品不存在
SC_B002=商品名稱已存在

# src/main/resources/messages_en.properties
SC_B001=Product not found
SC_B002=Product name already exists
```

**2. 自訂 ExceptionHandler 覆蓋預設**

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class I18nExceptionHandler {

    private final MessageSource messageSource;

    public I18nExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handle(BusinessException ex, Locale locale) {
        // 用 code 查翻譯，找不到就 fallback 到 enum 預設訊息
        String msg = messageSource.getMessage(ex.getCode(), null, ex.getMessage(), locale);
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getCode(), msg));
    }
}
```

**3. 客戶端帶 `Accept-Language` header 即可切換語系**

```bash
# 中文
curl -H "Accept-Language: zh-TW" /api/products/999
# → {"code": "SC_B001", "message": "商品不存在"}

# 英文
curl -H "Accept-Language: en" /api/products/999
# → {"code": "SC_B001", "message": "Product not found"}
```

> `ErrorCode` enum 裡的 message 作為 fallback，沒有翻譯檔的專案完全不受影響。

---

## 配置

### application.yml

```yaml
common:
  response:
    enabled: true                # 是否啟用（預設 true）
    exclude-paths:               # 排除路徑，Ant 風格（預設排除 /actuator/**）
      - /actuator/**
      - /internal/**
```

> 排除的路徑不會被 `GlobalExceptionHandler` 攔截，也不會被 `GlobalResponseAdvice` 自動包裝。

---

## 設計決策

### 要什麼

| 功能 | 原因 |
|------|------|
| 統一 `ApiResponse` 格式 | 前端只需處理一種回應結構 |
| 自動包裝（ResponseAdvice） | 減少每個 Controller 手動包裝的重複程式碼 |
| 錯誤碼分類（A/B/C/D） | 一看 code 就知道是用戶端、業務、系統還是外部問題 |
| `BusinessException` 語意化工廠 | `BusinessException.notFound("...")` 比 `new BusinessException(CommonErrorCode.NOT_FOUND, "...")` 更簡潔 |
| 路徑排除 | `/actuator/**` 等端點不應該被包裝 |
| 最低優先級 `@Order` | 讓安全 starter 先攔截認證/授權異常 |
| `@ConditionalOnMissingBean` | 消費端可以完全替換預設行為 |

### 不要什麼

| 功能 | 原因 |
|------|------|
| 內建 i18n 訊息 | 訊息國際化交由消費端決定 |
| 未知異常暴露 stack trace | 生產環境只回傳 `"系統內部錯誤"`，stack trace 記在 log |
| 自動 HTTP Status 映射 | 讓 `ErrorCode.getHttpStatus()` 明確定義，不做隱式推斷 |
| 統一 timestamp 欄位 | 增加 payload 大小，前端通常不需要 |

### 設計原則

1. **統一格式** — 所有 API 回傳 `{success, code, message, data}` 結構
2. **錯誤不隱藏** — 所有異常都有對應錯誤碼，不吞異常
3. **可擴展** — `ErrorCode` 介面 + `BusinessException` 支援任意自訂錯誤碼
4. **不衝突** — 最低優先級 + `@ConditionalOnMissingBean`，與其他 starter 和平共存
5. **安全** — 未知異常不暴露詳細訊息給前端

---

## 依賴關係

```
common-response-spring-boot-starter
├── spring-boot-starter-web (provided)
├── spring-boot-starter-validation (provided)
└── jackson-databind (provided)
```

---

## 專案結構與技術規格

### 目錄樹

```
common-response-spring-boot-starter/
├── src/main/java/com/company/common/response/
│   ├── code/
│   │   ├── ErrorCode.java                    # 錯誤碼介面（可擴展）
│   │   ├── CommonErrorCode.java              # 通用錯誤碼（A0xxx/B0xxx/C0xxx/D0xxx）
│   │   ├── UserErrorCode.java                # 用戶錯誤碼（USER_Bxxx）
│   │   ├── OrderErrorCode.java               # 訂單錯誤碼（ORDER_Bxxx）
│   │   └── FileErrorCode.java                # 檔案錯誤碼（FILE_Bxxx）
│   ├── config/
│   │   ├── ResponseAutoConfiguration.java    # 自動配置
│   │   └── ResponseProperties.java           # 配置屬性（common.response.*）
│   ├── dto/
│   │   ├── ApiResponse.java                  # 統一回應封裝
│   │   ├── FieldError.java                   # 欄位驗證錯誤
│   │   └── PageResponse.java                 # 分頁回應封裝
│   ├── exception/
│   │   └── BusinessException.java            # 業務異常（含語意化工廠方法）
│   └── handler/
│       ├── GlobalExceptionHandler.java       # 全局異常處理器（最低優先級）
│       └── GlobalResponseAdvice.java         # 自動回應包裝（ResponseBodyAdvice）
├── src/main/resources/META-INF/spring/
│   └── ...AutoConfiguration.imports          # AutoConfiguration SPI 註冊
└── pom.xml
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 自動配置 | `AutoConfiguration.imports`（Spring Boot 3.0+） |
| 條件 | `@ConditionalOnWebApplication(SERVLET)` |
| JSON 序列化 | Jackson (`JsonMapper`) |
| 回應包裝 | `ResponseBodyAdvice` |
| 異常處理 | `@RestControllerAdvice` + `@Order(LOWEST_PRECEDENCE)` |
| 路徑匹配 | `AntPathMatcher` |

---

## 版本

### 1.0.0

- `ApiResponse<T>` 統一回應封裝（成功/失敗/驗證錯誤）
- `PageResponse<T>` 分頁回應
- `ErrorCode` 介面 + `CommonErrorCode`、`UserErrorCode`、`OrderErrorCode`、`FileErrorCode`
- `BusinessException` 業務異常（含語意化靜態工廠方法）
- `GlobalExceptionHandler` 全局異常處理（12 種異常）
- `GlobalResponseAdvice` 自動回應包裝（String 特殊處理）
- 路徑排除（預設 `/actuator/**`）
- 零配置，引入即生效
