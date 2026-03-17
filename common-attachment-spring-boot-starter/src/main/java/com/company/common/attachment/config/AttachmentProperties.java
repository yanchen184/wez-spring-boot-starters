package com.company.common.attachment.config;

import com.company.common.attachment.storage.StorageType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "wez.attachment")
public class AttachmentProperties {

    /** 儲存策略：FILESYSTEM 或 DATABASE */
    private StorageType storageType = StorageType.FILESYSTEM;

    /** 檔案系統儲存根目錄 */
    private String storagePath = "./attachments";

    /** 最大檔案大小（預設 50MB） */
    private DataSize maxFileSize = DataSize.ofMegabytes(50);

    /** 允許的 MIME types（空表示不限制，由副檔名 blocklist 把關） */
    private Set<String> allowedMimeTypes;

    /** 是否啟用 REST controller */
    private Web web = new Web();

    /** 圖片處理設定 */
    private Image image = new Image();

    @Getter
    @Setter
    public static class Web {
        private boolean enabled = false;
    }

    @Getter
    @Setter
    public static class Image {
        /** 是否啟用上傳後自動壓縮 */
        private boolean compressionEnabled = true;

        /** 壓縮品質 (0.0 ~ 1.0) */
        private double quality = 0.8;

        /** 縮放比例 (1.0 = 不縮放) */
        private double scale = 1.0;

        /** 超過此閾值才壓縮（預設 500KB） */
        private DataSize compressionThreshold = DataSize.ofKilobytes(500);
    }
}
