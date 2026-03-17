package com.company.common.attachment.processing;

import com.company.common.attachment.config.AttachmentProperties;
import com.company.common.attachment.event.AttachmentUploadedEvent;
import com.company.common.attachment.persistence.entity.AttachmentEntity;
import com.company.common.attachment.storage.AttachmentStorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class ImageProcessingService {

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    private final AttachmentStorageStrategy storageStrategy;
    private final AttachmentProperties properties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAttachmentUploaded(AttachmentUploadedEvent event) {
        AttachmentEntity attachment = event.getAttachment();
        if (!properties.getImage().isCompressionEnabled()) {
            return;
        }
        if (!isImage(attachment.getMimeType())) {
            return;
        }
        // 小於閾值不壓縮
        if (attachment.getFileSize() < properties.getImage().getCompressionThreshold().toBytes()) {
            log.debug("圖片 {} 小於壓縮閾值，跳過壓縮", attachment.getStoredFilename());
            return;
        }

        try {
            compressImage(attachment);
        } catch (IOException e) {
            log.error("圖片壓縮失敗: {} (id={})", attachment.getStoredFilename(), attachment.getId(), e);
        }
    }

    private void compressImage(AttachmentEntity attachment) throws IOException {
        log.info("開始壓縮圖片: {} (id={}, size={} bytes)",
                attachment.getStoredFilename(), attachment.getId(), attachment.getFileSize());

        try (InputStream original = storageStrategy.load(attachment.getStoredFilename())) {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            Thumbnails.of(original)
                    .scale(properties.getImage().getScale())
                    .outputQuality(properties.getImage().getQuality())
                    .toOutputStream(compressed);

            // 只有壓縮後確實變小了才回寫
            byte[] compressedBytes = compressed.toByteArray();
            if (compressedBytes.length < attachment.getFileSize()) {
                storageStrategy.store(
                        attachment.getStoredFilename(),
                        new ByteArrayInputStream(compressedBytes)
                );
                log.info("圖片壓縮完成: {} -> {} bytes (節省 {}%)",
                        attachment.getFileSize(), compressedBytes.length,
                        (1 - (double) compressedBytes.length / attachment.getFileSize()) * 100);
            } else {
                log.info("壓縮後檔案未縮小，保留原始檔案: {}", attachment.getStoredFilename());
            }
        }
    }

    private boolean isImage(String mimeType) {
        return mimeType != null && SUPPORTED_IMAGE_TYPES.contains(mimeType.toLowerCase());
    }
}
