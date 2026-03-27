package com.company.common.attachment.processing;

import com.company.common.attachment.config.AttachmentProperties;
import com.company.common.attachment.event.AttachmentUploadedEvent;
import com.company.common.attachment.persistence.repository.AttachmentRepository;
import com.company.common.attachment.storage.AttachmentStorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
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
    private final AttachmentRepository attachmentRepository;
    private final AttachmentProperties properties;

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAttachmentUploaded(AttachmentUploadedEvent event) {
        if (!properties.getImage().isCompressionEnabled()) {
            return;
        }
        if (!isImage(event.getMimeType())) {
            return;
        }
        if (event.getFileSize() < properties.getImage().getCompressionThreshold().toBytes()) {
            log.debug("圖片 {} 小於壓縮閾值，跳過壓縮", event.getStoredFilename());
            return;
        }

        try {
            compressImage(event.getAttachmentId(), event.getStoredFilename(), event.getFileSize());
        } catch (IOException e) {
            log.error("圖片壓縮失敗: {} (id={})", event.getStoredFilename(), event.getAttachmentId(), e);
        }
    }

    private void compressImage(Long attachmentId, String storedFilename, long originalSize)
            throws IOException {
        log.info("開始壓縮圖片: {} (id={}, size={} bytes)", storedFilename, attachmentId, originalSize);

        try (InputStream original = storageStrategy.load(storedFilename)) {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            Thumbnails.of(original)
                    .scale(properties.getImage().getScale())
                    .outputQuality(properties.getImage().getQuality())
                    .toOutputStream(compressed);

            byte[] compressedBytes = compressed.toByteArray();
            if (compressedBytes.length >= originalSize) {
                log.info("壓縮後檔案未縮小，保留原始檔案: {}", storedFilename);
                return;
            }

            // 刪除原檔，存入壓縮後的檔案（用相同 storedFilename）
            storageStrategy.delete(storedFilename);
            storageStrategy.store(storedFilename, new ByteArrayInputStream(compressedBytes));

            // 更新 DB 的 fileSize
            attachmentRepository.findById(attachmentId).ifPresent(entity -> {
                entity.setFileSize((long) compressedBytes.length);
                attachmentRepository.save(entity);
            });

            long savedPercent = (originalSize - compressedBytes.length) * 100 / originalSize;
            log.info("圖片壓縮完成: {} -> {} bytes (節省 {}%)",
                    originalSize, compressedBytes.length, savedPercent);
        }
    }

    private boolean isImage(String mimeType) {
        return mimeType != null && SUPPORTED_IMAGE_TYPES.contains(mimeType.toLowerCase());
    }
}
