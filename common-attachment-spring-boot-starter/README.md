# Common Attachment Spring Boot Starter

通用附件管理模組 — 檔案上傳/下載/軟刪除，支援檔案系統與資料庫雙儲存策略

---

## 功能總覽

- **上傳** — Tika 偵測真實 MIME type、檔案大小限制、副檔名黑名單（26 種）、路徑穿越防護、雙重副檔名攻擊防護
- **下載** — streaming 串流，不吃 heap
- **軟刪除** — 繼承 `BaseEntity`，搭配 `SoftDeleteRepository`
- **儲存策略** — filesystem / database blob 可切換（`@ConditionalOnProperty`）
- **存取控制** — `AttachmentAccessPolicy` 介面，消費端必須實作
- **圖片壓縮** — 上傳後異步壓縮（`@TransactionalEventListener`，Thumbnailator），壓縮後自動更新 DB fileSize
- **事件驅動** — `AttachmentUploadedEvent` / `AttachmentDeletedEvent`（傳 ID，不傳 Entity）

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-attachment-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 實作 AttachmentAccessPolicy

**必須實作**，否則預設 `DefaultDenyAccessPolicy` 會拒絕所有存取。

```java
@Component
public class MyAttachmentAccessPolicy implements AttachmentAccessPolicy {

    @Override
    public boolean canAccess(AttachmentEntity attachment) {
        // 登入即可存取
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    @Override
    public boolean canDelete(AttachmentEntity attachment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        // 上傳者本人可刪
        if (auth.getName().equals(attachment.getCreatedBy())) return true;

        // ADMIN 可刪
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
```

### 3. 設定 application.yml

```yaml
wez:
  attachment:
    storage-type: filesystem          # filesystem | database
    storage-path: ./attachments       # filesystem 模式的儲存路徑
    max-file-size: 10MB
    allowed-mime-types:               # 空 = 不限制
      - image/jpeg
      - image/png
      - application/pdf
      - text/plain
```

### 4. 啟用異步（圖片壓縮需要）

```java
@SpringBootApplication
@EnableAsync          // ← 加這個，圖片壓縮才會異步執行
public class MyApplication { ... }
```

> Starter 不會自動啟用 `@EnableAsync`，避免影響整個應用。

### 5. 使用 AttachmentService

```java
@RestController
@RequiredArgsConstructor
public class MyController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    public AttachmentUploadResponse upload(@RequestParam MultipartFile file) throws IOException {
        return attachmentService.upload(new AttachmentUploadRequest(
                "ORDER", 123L,
                file.getOriginalFilename(), null,
                file.getInputStream(), file.getSize(),
                file.getContentType()
        ));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) throws IOException {
        AttachmentDownloadResponse resp = attachmentService.download(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + resp.originalFilename() + "\"")
                .contentType(MediaType.parseMediaType(resp.mimeType()))
                .contentLength(resp.fileSize())
                .body(new InputStreamResource(resp.inputStream()));
    }
}
```

---

## AttachmentService API

| 方法 | 回傳 | 說明 |
|------|------|------|
| `upload(request)` | `AttachmentUploadResponse` | 上傳（自動驗證 MIME、大小、路徑） |
| `download(id)` | `AttachmentDownloadResponse` | 下載（streaming，含權限檢查） |
| `softDelete(id)` | `void` | 軟刪除（含權限檢查） |
| `findByOwner(ownerRef)` | `List<AttachmentUploadResponse>` | 依 owner 查詢附件列表 |
| `findById(id)` | `AttachmentUploadResponse` | 查詢單一附件 metadata |

---

## 上傳 Pipeline

每次上傳強制經過以下流程，不可跳過：

```
Request
  → readAllBytes()             (讀入 byte[]，避免 stream 被 Tika 消耗後截斷)
  → Tika.detect()              (偵測真實 MIME type，不信任 client)
  → PathTraversalGuard         (order=50，路徑安全 + 副檔名黑名單 + 雙重副檔名防護)
  → FileSizeValidator          (order=100，用實際 byte[] 大小驗證，非宣告值)
  → MimeTypeValidator          (order=200，case-insensitive 比對白名單)
  → StorageStrategy.store()    (存檔)
  → Persistence                (寫 DB)
  → AttachmentUploadedEvent    (commit 後觸發，異步圖片壓縮)
  → Response
```

---

## 安全防護

### MIME 偵測

用 Apache Tika 讀取檔案的 **magic bytes** 判斷真實類型，不看副檔名。

```
virus.exe 改名成 photo.jpg → Tika 偵測為 application/x-dosexec → 拒絕
```

MIME 比對為 **case-insensitive**（`image/JPEG` 等同 `image/jpeg`）。

### 副檔名黑名單（26 種）

```
.exe .bat .cmd .sh .ps1 .vbs .js .jar .war .class
.msi .dll .so .py .rb .php .asp .aspx .jsp .cgi
.com .scr .pif .hta .wsf .mjs
```

### 雙重副檔名攻擊防護

```
malware.exe.pdf → 檢測到 .exe. 在中間 → 拒絕
```

不只檢查結尾，也檢查檔名中間是否包含 `.blocked_ext.`。

### 其他防護

- **路徑穿越** — 拒絕 `..`、`/`、`\`
- **Null byte** — 拒絕 `\0`
- **Trailing dot** — 拒絕 `file.exe.`（Windows 會自動移除結尾 `.`，繞過檢查）
- **儲存層** — `Path.normalize()` + `startsWith` 二次檢查

### 檔案大小驗證

用 **實際讀取的 byte[] 大小** 驗證，不信任 client 宣告的 `fileSize`。即使 caller 傳 `fileSize=0`，真正的檔案大小仍會被檢查。

---

## 設定參數

```yaml
wez:
  attachment:
    storage-type: filesystem        # filesystem | database
    storage-path: ./attachments     # 儲存路徑（filesystem 模式）
    max-file-size: 50MB             # 最大檔案大小（預設 50MB）
    allowed-mime-types:             # 空 = 不限制 MIME
      - image/jpeg
      - application/pdf
    web:
      enabled: false                # 是否自動註冊 REST controller
    image:
      compression-enabled: true     # 是否啟用圖片壓縮
      quality: 0.8                  # 壓縮品質 (0.0 ~ 1.0)
      scale: 1.0                    # 縮放比例
      compression-threshold: 500KB  # 超過此大小才壓縮
```

---

## 儲存策略

### Filesystem（預設）

```yaml
wez:
  attachment:
    storage-type: filesystem
    storage-path: /data/attachments
```

- 檔案以 UUID 重命名，防止檔名衝突
- 路徑穿越防護（`Path.normalize()` + `startsWith` 檢查）

### Database BLOB

```yaml
wez:
  attachment:
    storage-type: database
```

- 存入 `AttachmentBlobEntity`，`@Lob @Basic(fetch = LAZY)` 延遲載入
- 適合不方便掛 NFS/S3 的環境
- 注意：大檔案會全量載入 heap，建議搭配較小的 `max-file-size`

### 自訂儲存策略

實作 `AttachmentStorageStrategy` 介面即可（例如 S3）：

```java
@Component
public class S3StorageStrategy implements AttachmentStorageStrategy {
    @Override
    public StorageResult store(String filename, InputStream is) throws IOException { ... }
    @Override
    public InputStream load(String storedFilename) throws IOException { ... }
    @Override
    public void delete(String storedFilename) throws IOException { ... }
    @Override
    public StorageType getStorageType() { return StorageType.FILESYSTEM; }
}
```

定義後預設策略自動停用（`@ConditionalOnMissingBean`）。

---

## Entity 結構

```
AttachmentEntity extends BaseEntity
├── (繼承) createdDate, lastModifiedDate, createdBy, lastModifiedBy
├── (繼承) deleted, version
├── id (Long, IDENTITY)
├── ownerType (String)       ← 業務類型（ORDER, CUSTOMER...）
├── ownerId (Long)           ← 業務 ID
├── originalFilename (String)
├── storedFilename (String)  ← UUID 重命名後的檔名
├── extension (String)
├── displayName (String)
├── mimeType (String)        ← Tika 偵測結果
├── fileSize (Long)          ← 實際大小（壓縮後會自動更新）
└── storageType (StorageType) ← FILESYSTEM / DATABASE
```

---

## 擴充點

| 介面 | 用途 | 預設 |
|------|------|------|
| `AttachmentAccessPolicy` | 存取/刪除權限 | `DefaultDenyAccessPolicy`（全拒絕） |
| `AttachmentStorageStrategy` | 儲存實作 | `FilesystemStorageStrategy` |
| `AttachmentValidator` | 自訂驗證規則 | MIME + size + path traversal |

所有擴充點都用 `@ConditionalOnMissingBean`，消費端定義自己的 Bean 即可替換。

---

## 事件

事件只傳 ID 和基本資訊，**不傳 JPA Entity**，避免 detached entity 問題。

| 事件 | 欄位 | 觸發時機 | 用途 |
|------|------|---------|------|
| `AttachmentUploadedEvent` | `attachmentId`, `storedFilename`, `mimeType`, `fileSize` | 上傳成功後（commit 後） | 圖片壓縮、通知、索引 |
| `AttachmentDeletedEvent` | `attachmentId`, `storedFilename` | 軟刪除成功後 | 清理、通知 |

監聽範例：

```java
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onUploaded(AttachmentUploadedEvent event) {
    log.info("附件已上傳: id={}, mimeType={}", event.getAttachmentId(), event.getMimeType());
}
```

---

## 圖片壓縮

- 使用 Thumbnailator，支援 JPEG / PNG / GIF / BMP / WebP
- 上傳成功 commit 後，由 `@TransactionalEventListener` 異步觸發
- 小於 `compression-threshold`（預設 500KB）不壓縮
- 壓縮後如果沒變小，保留原檔
- 壓縮後自動更新 DB 的 `fileSize`

**前提：** 消費端必須在 `@SpringBootApplication` 上加 `@EnableAsync`。

---

## 專案結構

```
common-attachment-spring-boot-starter/
├── pom.xml
│
├── src/main/java/com/company/common/attachment/
│   │
│   ├── config/                                     ← 自動配置
│   │   ├── AttachmentAutoConfiguration.java        # @AutoConfiguration，條件式註冊所有 Bean
│   │   └── AttachmentProperties.java               # @ConfigurationProperties(prefix = "wez.attachment")
│   │
│   ├── core/                                       ← 核心業務
│   │   ├── AttachmentService.java                  # 主 facade：upload/download/softDelete/findByOwner/findById
│   │   │                                           # upload 先 readAllBytes() 再分別給 Tika 偵測和 storage 存檔
│   │   ├── AttachmentNotFoundException.java        # 附件不存在例外
│   │   ├── AttachmentAccessDeniedException.java    # 無權存取例外
│   │   └── model/                                  ← DTO（全部 record）
│   │       ├── AttachmentUploadRequest.java        # 上傳請求（ownerType, ownerId, filename, inputStream, fileSize, contentType）
│   │       ├── AttachmentUploadResponse.java       # 上傳回應（id, ownerType, filename, mimeType, fileSize, storageType, createdDate）
│   │       ├── AttachmentDownloadResponse.java     # 下載回應（inputStream, filename, mimeType, fileSize）
│   │       └── AttachmentOwnerRef.java             # Owner 查詢條件（ownerType, ownerId）
│   │
│   ├── storage/                                    ← 儲存策略（Strategy Pattern）
│   │   ├── AttachmentStorageStrategy.java          # interface：store / load / delete / getStorageType
│   │   ├── FilesystemStorageStrategy.java          # 檔案系統實作（預設），UUID 重命名 + 路徑穿越防護
│   │   ├── DatabaseBlobStorageStrategy.java        # DB BLOB 實作，@ConditionalOnProperty 切換
│   │   ├── StorageResult.java                      # 儲存結果（storedFilename, storageType, fileSize）
│   │   └── StorageType.java                        # enum：FILESYSTEM / DATABASE
│   │
│   ├── validation/                                 ← 驗證 pipeline（強制執行，不可跳過）
│   │   ├── AttachmentValidator.java                # interface：validate(request) + getOrder()
│   │   ├── PathTraversalGuard.java                 # order=50，路徑穿越 + 26 種危險副檔名 + 雙重副檔名 + trailing dot
│   │   ├── FileSizeValidator.java                  # order=100，用實際 byte[] 大小驗證
│   │   ├── MimeTypeValidator.java                  # order=200，Tika 偵測 + case-insensitive 白名單比對
│   │   └── AttachmentValidationException.java      # 驗證失敗例外
│   │
│   ├── processing/                                 ← 後處理
│   │   └── ImageProcessingService.java             # @TransactionalEventListener + @Async，Thumbnailator 壓縮
│   │                                               # 壓縮後更新 DB fileSize，用 ID 載入 entity 避免 detached 問題
│   │
│   ├── security/                                   ← 存取控制
│   │   ├── AttachmentAccessPolicy.java             # interface：canAccess / canDelete（消費端實作）
│   │   └── DefaultDenyAccessPolicy.java            # 預設全拒絕，WARN log 提醒消費端實作
│   │
│   ├── persistence/                                ← JPA 持久層
│   │   ├── entity/
│   │   │   ├── AttachmentEntity.java               # extends BaseEntity，含 ownerType/ownerId 複合索引
│   │   │   └── AttachmentBlobEntity.java           # @Lob @Basic(fetch = LAZY)，DB 模式用
│   │   └── repository/
│   │       ├── AttachmentRepository.java           # extends SoftDeleteRepository，含 findByOwner 查詢
│   │       └── AttachmentBlobRepository.java       # JpaRepository<AttachmentBlobEntity, Long>
│   │
│   ├── web/                                        ← REST Controller（opt-in）
│   │   └── AttachmentController.java               # @ConditionalOnProperty(wez.attachment.web.enabled=true)
│   │                                               # POST /attachments, GET /attachments/{id},
│   │                                               # GET /attachments/{id}/download, DELETE /attachments/{id}
│   │
│   └── event/                                      ← Spring Application Event（傳 ID，不傳 Entity）
│       ├── AttachmentUploadedEvent.java            # attachmentId, storedFilename, mimeType, fileSize
│       └── AttachmentDeletedEvent.java             # attachmentId, storedFilename
│
├── src/main/resources/META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── src/test/java/com/company/common/attachment/    ← 30 個測試
    ├── TestApplication.java                        # @SpringBootApplication（測試用 context 根）
    ├── AttachmentIntegrationTest.java              # H2 整合測試：上傳→查詢→下載→刪除完整流程（10 個）
    ├── core/
    │   └── AttachmentServiceTest.java              # Mockito 單元測試：upload/download/delete/findByOwner（6 個）
    ├── storage/
    │   └── FilesystemStorageStrategyTest.java      # 儲存/讀取/刪除（3 個）
    └── validation/
        ├── FileSizeValidatorTest.java              # 大小限制（2 個）
        └── PathTraversalGuardTest.java             # 路徑穿越 + 危險副檔名 + 空檔名（9 個）
```

---

## 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 自動配置 | `AutoConfiguration.imports` |
| MIME 偵測 | Apache Tika 3.2.3 |
| 圖片壓縮 | Thumbnailator 0.4.20 |
| 依賴 | `common-jpa-spring-boot-starter`（BaseEntity, SoftDeleteRepository） |
| 前提 | 消費端需 `@EnableAsync`（圖片壓縮用） |

---

## 設計決策

### 為什麼先 readAllBytes()？

Tika 偵測 MIME type 時會消耗 stream 的前 N 個 bytes。如果用 `BufferedInputStream` + `mark/reset`，超過 buffer size 的檔案會截斷。先讀成 `byte[]` 再分別建 `ByteArrayInputStream` 是最安全的做法。

### 為什麼 Event 傳 ID 不傳 Entity？

`@TransactionalEventListener(AFTER_COMMIT)` 在 commit 後執行，此時原本的 persistence context 已關閉。如果傳 entity，`@Async` handler 在另一個 thread 存取 lazy 屬性會拋 `LazyInitializationException`。傳 ID 讓 handler 自己開 `@Transactional` 重新載入。

### 為什麼不在 Starter 裡加 @EnableAsync？

`@EnableAsync` 是全域設定，會影響整個應用的行為。Starter 不應該偷偷改變消費端的全域配置。

### 為什麼預設 Deny？

每個專案的權限規則不同（同單位才能看 vs 登入就能看 vs ADMIN 才能看）。如果預設 Allow，消費端忘記實作就是安全漏洞。預設 Deny + WARN log 提醒，確保不會默默放行。

---

## 版本

- 1.0.0
  - 上傳/下載/軟刪除/依 owner 查詢
  - Filesystem / Database BLOB 雙儲存策略
  - 強制驗證 pipeline（Tika MIME + 實際大小 + 路徑穿越 + 26 種副檔名黑名單 + 雙重副檔名防護）
  - MIME 比對 case-insensitive
  - AttachmentAccessPolicy 存取控制（預設全拒絕）
  - 圖片異步壓縮（Thumbnailator），壓縮後自動更新 DB fileSize
  - Event 傳 ID 不傳 Entity，避免 detached entity
  - 不自動啟用 @EnableAsync，不影響消費端全域配置
  - 30 個測試（單元 + H2 整合）
