# Common Attachment Spring Boot Starter

通用附件管理模組 — 檔案上傳/下載/軟刪除，支援檔案系統與資料庫雙儲存策略

---

## 功能總覽

- **上傳** — Tika 偵測真實 MIME type、檔案大小限制、副檔名黑名單、路徑穿越防護
- **下載** — streaming 串流，不吃 heap
- **軟刪除** — 繼承 `BaseEntity`，搭配 `SoftDeleteRepository`
- **儲存策略** — filesystem / database blob 可切換（`@ConditionalOnProperty`）
- **存取控制** — `AttachmentAccessPolicy` 介面，消費端必須實作
- **圖片壓縮** — 上傳後異步壓縮（`@Async` + `@TransactionalEventListener`，Thumbnailator）
- **事件驅動** — `AttachmentUploadedEvent` / `AttachmentDeletedEvent`

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
        // 根據 SecurityContext 判斷是否可存取
        return true;
    }

    @Override
    public boolean canDelete(AttachmentEntity attachment) {
        // 根據 SecurityContext 判斷是否可刪除
        return hasRole("ADMIN");
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

### 4. 使用 AttachmentService

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

每次上傳強制經過以下驗證，不可跳過：

```
Request
  → PathTraversalGuard        (order=50，路徑安全)
  → FileSizeValidator          (order=100，大小限制)
  → MimeTypeValidator          (order=200，Tika 偵測真實 MIME)
  → StorageStrategy.store()    (存檔)
  → Persistence                (寫 DB)
  → AttachmentUploadedEvent    (異步圖片壓縮)
  → Response
```

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

### 自訂儲存策略

實作 `AttachmentStorageStrategy` 介面即可（例如 S3）：

```java
@Component
public class S3StorageStrategy implements AttachmentStorageStrategy {
    @Override
    public StorageResult store(String filename, InputStream is) { ... }
    @Override
    public InputStream load(String storedFilename) { ... }
    @Override
    public void delete(String storedFilename) { ... }
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
├── fileSize (Long)
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

| 事件 | 觸發時機 | 用途 |
|------|---------|------|
| `AttachmentUploadedEvent` | 上傳成功後（commit 後） | 圖片壓縮、通知、索引 |
| `AttachmentDeletedEvent` | 軟刪除成功後 | 清理、通知 |

監聽範例：

```java
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onUploaded(AttachmentUploadedEvent event) {
    log.info("附件已上傳: {}", event.getAttachment().getOriginalFilename());
}
```

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
│   │   ├── PathTraversalGuard.java                 # order=50，路徑穿越 + 危險副檔名檢查
│   │   ├── FileSizeValidator.java                  # order=100，檔案大小限制
│   │   ├── MimeTypeValidator.java                  # order=200，Tika 偵測真實 MIME type + 白名單比對
│   │   └── AttachmentValidationException.java      # 驗證失敗例外
│   │
│   ├── processing/                                 ← 後處理
│   │   └── ImageProcessingService.java             # @Async + @TransactionalEventListener，Thumbnailator 壓縮
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
│   └── event/                                      ← Spring Application Event
│       ├── AttachmentUploadedEvent.java            # 上傳成功後發布（commit 後觸發圖片壓縮）
│       └── AttachmentDeletedEvent.java             # 軟刪除後發布
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

---

## 版本

- 1.0.0
  - 上傳/下載/軟刪除/依 owner 查詢
  - Filesystem / Database BLOB 雙儲存策略
  - 強制驗證 pipeline（Tika MIME + 大小 + 路徑穿越）
  - AttachmentAccessPolicy 存取控制
  - 圖片異步壓縮（Thumbnailator）
  - Spring Application Event
  - 30 個測試（單元 + H2 整合）
