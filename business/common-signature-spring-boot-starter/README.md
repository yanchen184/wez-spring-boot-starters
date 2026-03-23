# Common Signature Spring Boot Starter

電子簽名板模組 — Fabric.js Canvas 簽名 JSON 存儲 + 附件圖片管理，依賴 common-attachment。

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

| 功能 | 說明 |
|------|------|
| 簽名儲存 | Fabric.js Canvas JSON + 截圖 PNG 附件 |
| REST API | POST / GET / DELETE 簽名，multipart 上傳 |
| 軟刪除 | 重簽時舊簽名標記 deleted，歷史完整保留可追溯 |
| 審計欄位 | 自動記錄 createdBy / createdDate / lastModifiedBy / lastModifiedDate |
| 零配置 | 引入依賴即生效，`@ConditionalOnBean` 自動偵測附件模組 |

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-signature-spring-boot-starter</artifactId>
</dependency>
```

> 需同時引入 `common-attachment-spring-boot-starter`（簽名圖片由附件模組管理）。

### 2. 完成

REST API 自動註冊，不需要任何配置。

### 最小使用範例

```bash
# 儲存簽名
curl -X POST /api/signatures \
  -F "ownerType=CONTRACT" \
  -F "ownerId=123" \
  -F "json={\"objects\":[...]}" \
  -F "image=@signature.png"

# 查詢簽名
curl /api/signatures?ownerType=CONTRACT&ownerId=123

# 刪除簽名
curl -X DELETE /api/signatures?ownerType=CONTRACT&ownerId=123
```

---

## 功能總覽

- **簽名存檔** — Canvas JSON + 截圖 PNG 一次儲存，透過 multipart/form-data 上傳
- **ownerType + ownerId 綁定** — 一個簽名板綁定一個業務物件（合約、表單等）
- **軟刪除** — 重簽時舊簽名整筆軟刪除（content + 附件都保留），新建一筆
- **附件整合** — 圖片儲存復用 attachment-starter，自動加上 `SIGN_` 前綴區分
- **審計追蹤** — 繼承 BaseEntity，自動記錄建立/修改者與時間
- **條件啟用** — `@ConditionalOnBean(AttachmentService)` 確保附件模組存在才啟用

---

## 核心 API

### REST API

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/signatures` | 儲存簽名（multipart：JSON + 圖片） |
| GET | `/api/signatures?ownerType={type}&ownerId={id}` | 查詢簽名 |
| DELETE | `/api/signatures?ownerType={type}&ownerId={id}` | 刪除簽名（軟刪除） |

### SignatureService

```java
// 儲存簽名（舊簽名自動軟刪除 + 新建一筆）
SignatureResponse save(SignatureSaveRequest request, MultipartFile image)

// 依 ownerType + ownerId 查詢有效簽名
SignatureResponse findByOwner(String ownerType, Long ownerId)

// 軟刪除簽名（簽名 + 附件都軟刪除）
void delete(String ownerType, Long ownerId)

// 取得簽名關聯的附件列表
List<AttachmentUploadResponse> getAttachments(String ownerType, Long ownerId)
```

### SignatureController

```java
@RestController
@RequestMapping("${common.signature.api-prefix:/api/signatures}")
public class SignatureController {

    @PostMapping
    ResponseEntity<SignatureResponse> save(
        @RequestParam String ownerType,
        @RequestParam Long ownerId,
        @RequestParam String json,
        @RequestPart(required = false) MultipartFile image)

    @GetMapping
    ResponseEntity<SignatureResponse> find(
        @RequestParam String ownerType,
        @RequestParam Long ownerId)

    @DeleteMapping
    ResponseEntity<Void> delete(
        @RequestParam String ownerType,
        @RequestParam Long ownerId)
}
```

### DTO

```java
// 請求
record SignatureSaveRequest(String ownerType, Long ownerId, String json)

// 回應
record SignatureResponse(
    Long id, String ownerType, Long ownerId,
    String content, Long attachmentId,
    String createdBy, LocalDateTime createdDate,
    String lastModifiedBy, LocalDateTime lastModifiedDate)
```

### Entity

```java
@Entity
@Table(name = "SIGNATURE_DIAGRAM")
public class SignatureDiagram extends BaseEntity {
    Long id;              // PK (IDENTITY)
    String ownerType;     // 業務表名，最長 50
    Long ownerId;         // 業務資料 ID
    String content;       // Fabric.js Canvas JSON (NVARCHAR(MAX))
    Long attachmentId;    // 關聯附件 ID
    // 繼承：deleted, version, createdBy, createdDate, lastModifiedBy, lastModifiedDate
}
```

### Repository

```java
public interface SignatureDiagramRepository extends SoftDeleteRepository<SignatureDiagram, Long> {
    // 查詢未刪除的有效簽名
    Optional<SignatureDiagram> findActiveByOwner(String ownerType, Long ownerId)
    // 查詢是否存在未刪除的簽名
    boolean existsActiveByOwner(String ownerType, Long ownerId)
}
```

---

## 配置

```yaml
common:
  signature:
    enabled: true                    # 是否啟用簽名板功能（預設 true）
    api-prefix: /api/signatures      # REST API 路徑前綴（預設 /api/signatures）
```

### 自動配置條件

| 條件 | 說明 |
|------|------|
| `common.signature.enabled=true` | 預設啟用，設為 false 可完全關閉 |
| `AttachmentService` Bean 存在 | 沒有附件模組就不啟用 SignatureService |
| `SignatureService` Bean 存在 | 沒有 Service 就不註冊 Controller |

---

## 設計決策

| 要 | 不要 |
|----|------|
| 軟刪除舊簽名 + 新建 | 覆蓋更新（保留歷史記錄） |
| ownerType + ownerId 組合鍵 | 獨立的簽名 ID 查詢（綁定業務物件） |
| 依賴 attachment-starter | 自己實作檔案儲存（復用不重複） |
| `@ConditionalOnBean` 條件啟用 | 強制要求附件模組（優雅降級） |
| 附件 ownerType 加 `SIGN_` 前綴 | 共用業務 ownerType（避免衝突） |
| BaseEntity 繼承 | 自己管理審計欄位（統一規範） |

---

## 依賴關係

```
common-signature-spring-boot-starter
├── common-jpa-spring-boot-starter          ← Entity 審計 + 軟刪除 + BaseEntity
├── common-attachment-spring-boot-starter   ← 圖片附件管理（上傳/下載/軟刪除）
├── spring-boot-starter-data-jpa            ← JPA Repository (provided)
└── spring-boot-starter-web                 ← REST Controller (provided)
```

---

## 專案結構與技術規格

### 目錄樹

```
common-signature-spring-boot-starter/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/company/common/signature/
    │   │   ├── config/
    │   │   │   ├── SignatureAutoConfiguration.java   # 自動配置
    │   │   │   └── SignatureProperties.java          # 配置屬性
    │   │   ├── dto/
    │   │   │   ├── SignatureSaveRequest.java          # 儲存請求 DTO
    │   │   │   └── SignatureResponse.java             # 回應 DTO
    │   │   ├── entity/
    │   │   │   └── SignatureDiagram.java              # JPA Entity
    │   │   ├── repository/
    │   │   │   └── SignatureDiagramRepository.java    # JPA Repository
    │   │   ├── service/
    │   │   │   └── SignatureService.java              # 核心業務邏輯
    │   │   └── web/
    │   │       └── SignatureController.java           # REST Controller
    │   └── resources/META-INF/spring/
    │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        └── java/com/company/common/signature/service/
            └── SignatureServiceTest.java              # 單元測試
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 資料庫表 | `SIGNATURE_DIAGRAM` |
| 主鍵策略 | `IDENTITY` |
| Canvas JSON 欄位 | `NVARCHAR(MAX)` |
| 軟刪除 | 繼承 `BaseEntity.deleted` |
| 自動配置 | `AutoConfiguration.imports` |

---

## 版本

- **1.0.0** — 初始版本：Canvas JSON 簽名、REST API、軟刪除、附件整合
- **1.0.0-fix** — 重簽改為軟刪除舊簽名 + 新建，不再覆蓋
