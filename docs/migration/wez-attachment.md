# wez-attachment → common-attachment-starter

> 狀態：❌ 待建
> 預估工作量：3-5 天
> 原始路徑：`plugin/wez-attachment`

## 原始模組分析

- **檔案數**: 11 個（全部 Groovy）
- **外部依賴**: Apache Tika 3.2.3、wez-base、wez-logs
- **功能**: 通用附件管理（上傳/下載/刪除/預覽/圖片壓縮/MIME 驗證）

## 現有架構問題（改寫時必須修正）

### P0 — 安全 & 記憶體

| # | 問題 | 現況 | 改法 |
|---|------|------|------|
| 1 | MIME 驗證沒有強制執行 | `WezFileValidationService.isAllowed()` 存在但 `save()` 沒呼叫，繞過 controller 就能傳 .exe/.jsp | 改成 pipeline，驗證是強制步驟不可跳過 |
| 2 | BLOB 全量載入記憶體 | `blob.blobContent = file.bytes` 上傳 100MB = 100MB 吃 heap | 用 JdbcTemplate `setBinaryStream` streaming；Entity BLOB 改 `FetchType.LAZY` |
| 3 | 儲存模式沒有抽象 | `if (uploadDb)` 硬切 filesystem vs DB，加 S3 就得改核心 | 抽 `AttachmentStorageStrategy` 介面，`@ConditionalOnProperty` 切換 |

### P1 — 安全 & 可維護

| # | 問題 | 現況 | 改法 |
|---|------|------|------|
| 4 | 下載沒有存取控制 | 給 attachment ID 就能下載，不檢查權限 | 抽 `AttachmentAccessPolicy` 介面，預設 deny，使用端必須實作 |
| 5 | 無型別安全 | 所有方法參數和回傳都是 `def` | 用 Request/Response DTO（record） |
| 6 | 圖片壓縮同步阻塞 | 大圖壓縮卡住 request thread | 先存原圖回應成功，壓縮用 `@Async` 或 `@TransactionalEventListener` 異步 |
| 7 | Extension 取法不安全 | `tokenize('.').last()` — `file.tar.gz` → `gz`，沒檔名時 NPE | 用 Tika 偵測真實 MIME 再反查 extension |
| 8 | 路徑安全 | `new File("${uploadPath}/${serverName}")` 沒做 path traversal 檢查 | 加 `Path.normalize()` + startsWith 檢查 |

### P2 — 優化

| # | 問題 | 現況 | 改法 |
|---|------|------|------|
| 9 | 軟刪除不完整 | 只有 `isDelete` flag，沒記錄誰刪的、何時刪的 | 加 `deletedAt` + `deletedBy` |
| 10 | Service 職責重疊 | `AttachmentService` vs `WezAttachmentService` 邊界模糊 | 拆成獨立元件：facade / validator / processor |
| 11 | ImageIO 局限 | 不支援 WebP/HEIC，大圖記憶體消耗高（4000x3000 ≈ 48MB heap） | 改用 Thumbnailator 或 imgscalr |
| 12 | 查詢缺複合索引 | `tabName + refid + isDelete` 篩選沒有對應索引 | Entity 加 `@Index` |

## Starter 設計

### 擴充點（使用端可替換）

| 介面 | 用途 | 預設實作 |
|------|------|---------|
| `AttachmentStorageStrategy` | 儲存位置 | `FilesystemStorage` |
| `AttachmentAccessPolicy` | 存取控制 | `DefaultDenyPolicy`（**強制使用端實作**） |
| `AttachmentNamingStrategy` | 檔名產生 | UUID |
| `AttachmentValidator` | 自訂驗證 | MIME + size |

### Configuration

```yaml
wez:
  attachment:
    storage-type: filesystem   # filesystem | database | s3
    upload-path: /data/attachments
    max-file-size: 10MB
    allowed-mime-types:
      - image/jpeg
      - image/png
      - application/pdf
    blocked-extensions:
      - exe
      - sh
      - jsp
      - bat
    soft-delete:
      enabled: true
    compression:
      enabled: true
      async: true
      max-width: 1920
      max-height: 1080
      quality: 0.85
    web:
      enabled: false           # 是否自動註冊 REST controller
```

### 上傳流程 Pipeline

```
Request
  → FileSizeValidator          (超過上限直接拒絕)
  → MimeTypeValidator          (Tika 偵測真實 MIME)
  → ExtensionBlocklistCheck    (黑名單)
  → PathTraversalGuard         (路徑安全)
  → StorageStrategy.store()    (存檔)
  → Persistence                (寫 DB)
  → 發布 AttachmentUploadedEvent
      → ImageProcessingService (異步壓縮，if 圖片)
  → Response
```

### Package 結構

```
common-attachment-starter/
├── src/main/java/tw/com/wezoomtek/attachment/
│   ├── autoconfigure/
│   │   ├── AttachmentAutoConfiguration.java
│   │   └── AttachmentProperties.java
│   │
│   ├── core/
│   │   ├── AttachmentService.java                ← 主 facade
│   │   ├── AttachmentUploadPipeline.java         ← 流程編排
│   │   └── model/
│   │       ├── AttachmentUploadRequest.java       (record)
│   │       ├── AttachmentUploadResponse.java      (record)
│   │       ├── AttachmentDownloadResponse.java    (record)
│   │       └── AttachmentOwnerType.java           (enum，取代 tabName 字串)
│   │
│   ├── storage/
│   │   ├── AttachmentStorageStrategy.java         ← interface
│   │   ├── FilesystemStorage.java
│   │   ├── DatabaseBlobStorage.java
│   │   └── StorageResult.java
│   │
│   ├── validation/
│   │   ├── AttachmentValidator.java               ← interface
│   │   ├── MimeTypeValidator.java                 (Tika)
│   │   ├── FileSizeValidator.java
│   │   └── ExtensionBlocklistValidator.java
│   │
│   ├── processing/
│   │   └── ImageProcessingService.java            (Thumbnailator，異步)
│   │
│   ├── security/
│   │   ├── AttachmentAccessPolicy.java            ← interface
│   │   ├── DefaultDenyAccessPolicy.java
│   │   └── PathTraversalGuard.java
│   │
│   ├── persistence/
│   │   ├── entity/
│   │   │   ├── AttachmentEntity.java
│   │   │   └── AttachmentBlobEntity.java
│   │   └── repository/
│   │       ├── AttachmentRepository.java
│   │       └── AttachmentBlobRepository.java
│   │
│   ├── web/                                        (opt-in)
│   │   ├── AttachmentController.java
│   │   └── AttachmentExceptionHandler.java
│   │
│   └── event/
│       ├── AttachmentUploadedEvent.java
│       └── AttachmentDeletedEvent.java
│
├── src/main/resources/META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── src/test/
```

### Entity 設計

```java
@Entity
@Table(name = "ATTACHMENT", indexes = {
    @Index(name = "IDX_ATT_OWNER", columnList = "OWNER_TYPE, OWNER_ID, DELETED_AT")
})
public class AttachmentEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "OWNER_TYPE", nullable = false, length = 30)
    private AttachmentOwnerType ownerType;    // 取代 tabName 字串

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;                     // 取代 refid

    @Column(name = "UPLOAD_USER_ID")
    private Long uploadUserId;

    @Column(name = "ORIGINAL_FILENAME", nullable = false)
    private String originalFilename;

    @Column(name = "STORED_FILENAME", nullable = false)
    private String storedFilename;            // UUID，取代 serverName

    @Column(name = "EXTENSION", length = 20)
    private String extension;

    @Column(name = "DISPLAY_NAME")
    private String displayName;               // 取代 showfilename

    @Column(name = "MIME_TYPE", length = 100)
    private String mimeType;                  // 新增，Tika 偵測結果

    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;

    @Column(name = "STORAGE_TYPE", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StorageType storageType;          // 新增，記錄存在哪

    @Column(name = "COMPRESSED")
    private Boolean compressed = false;

    // 軟刪除增強
    @Column(name = "DELETED_AT")
    private Instant deletedAt;

    @Column(name = "DELETED_BY")
    private Long deletedBy;
}
```

### 與舊欄位對照

| 舊 (Grails) | 新 (Spring Boot) | 變更理由 |
|---|---|---|
| `tabName` (String) | `ownerType` (Enum) | 防止任意字串，限定合法類型 |
| `refid` | `ownerId` | 更語義化 |
| `serverName` | `storedFilename` | 更語義化 |
| `showfilename` | `displayName` | 更語義化 |
| `isDelete` (Boolean) | `deletedAt` (Instant) | 可追溯刪除時間，null = 未刪除 |
| — | `mimeType` | 新增，Tika 偵測存入 |
| — | `storageType` | 新增，知道檔案存在哪 |
| — | `deletedBy` | 新增，知道誰刪的 |

## 遷移步驟

1. 建立 Maven module `common-attachment-spring-boot-starter`
2. 定義 `AttachmentProperties`（ConfigurationProperties）
3. 實作 `AttachmentStorageStrategy` 介面 + Filesystem / DB 兩個實作
4. 實作 validation pipeline（Tika + size + blocklist）
5. 建立 JPA Entity + Repository
6. 實作 `AttachmentUploadPipeline`（編排驗證→儲存→持久化→事件）
7. 實作 `ImageProcessingService`（異步，Thumbnailator）
8. 定義 `AttachmentAccessPolicy` 介面
9. 可選：`AttachmentController`（`@ConditionalOnProperty`）
10. AutoConfiguration + 測試

## 依賴

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>3.2.3</version>
    </dependency>
    <dependency>
        <groupId>net.coobird</groupId>
        <artifactId>thumbnailator</artifactId>
        <version>0.4.20</version>
    </dependency>
    <!-- common-jpa-spring-boot-starter (BaseEntity, 審計) -->
    <!-- spring-boot-starter-data-jpa -->
    <!-- spring-boot-starter-web (optional, for controller) -->
</dependencies>
```
