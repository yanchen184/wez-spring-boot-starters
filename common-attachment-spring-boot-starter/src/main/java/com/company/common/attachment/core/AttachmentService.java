package com.company.common.attachment.core;

import com.company.common.attachment.config.AttachmentProperties;
import com.company.common.attachment.core.model.AttachmentDownloadResponse;
import com.company.common.attachment.core.model.AttachmentOwnerRef;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import com.company.common.attachment.event.AttachmentDeletedEvent;
import com.company.common.attachment.event.AttachmentUploadedEvent;
import com.company.common.attachment.persistence.entity.AttachmentEntity;
import com.company.common.attachment.persistence.repository.AttachmentRepository;
import com.company.common.attachment.security.AttachmentAccessPolicy;
import com.company.common.attachment.storage.AttachmentStorageStrategy;
import com.company.common.attachment.storage.StorageResult;
import com.company.common.attachment.validation.AttachmentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorageStrategy storageStrategy;
    private final AttachmentAccessPolicy accessPolicy;
    private final List<AttachmentValidator> validators;
    private final ApplicationEventPublisher eventPublisher;
    private final AttachmentProperties properties;

    private final Tika tika = new Tika();

    @Transactional
    public AttachmentUploadResponse upload(AttachmentUploadRequest request) throws IOException {
        // 用 Tika 偵測真實 MIME type（而非信任 client 提供的 contentType）
        InputStream buffered = new BufferedInputStream(request.inputStream());
        String detectedMimeType = tika.detect(buffered, request.originalFilename());

        AttachmentUploadRequest enrichedRequest = new AttachmentUploadRequest(
                request.ownerType(),
                request.ownerId(),
                request.originalFilename(),
                request.displayName(),
                buffered,
                request.fileSize(),
                detectedMimeType
        );

        // 驗證（依照 order 排序後逐一執行）
        validators.stream()
                .sorted(Comparator.comparingInt(AttachmentValidator::getOrder))
                .forEach(v -> v.validate(enrichedRequest));

        // 儲存檔案
        StorageResult result = storageStrategy.store(
                request.originalFilename(), enrichedRequest.inputStream());

        // 持久化 metadata
        AttachmentEntity entity = new AttachmentEntity();
        entity.setOwnerType(enrichedRequest.ownerType());
        entity.setOwnerId(enrichedRequest.ownerId());
        entity.setOriginalFilename(enrichedRequest.originalFilename());
        entity.setStoredFilename(result.storedFilename());
        entity.setExtension(extractExtension(enrichedRequest.originalFilename()));
        entity.setDisplayName(enrichedRequest.displayName() != null
                ? enrichedRequest.displayName() : enrichedRequest.originalFilename());
        entity.setMimeType(detectedMimeType);
        entity.setFileSize(result.fileSize());
        entity.setStorageType(result.storageType());

        entity = attachmentRepository.save(entity);
        log.info("附件上傳完成: id={}, filename={}, mimeType={}, size={} bytes",
                entity.getId(), entity.getOriginalFilename(), entity.getMimeType(), entity.getFileSize());

        // 發布事件（commit 後觸發圖片壓縮等後處理）
        eventPublisher.publishEvent(new AttachmentUploadedEvent(this, entity));

        return toUploadResponse(entity);
    }

    @Transactional(readOnly = true)
    public AttachmentDownloadResponse download(Long id) throws IOException {
        AttachmentEntity entity = findActiveOrThrow(id);
        if (!accessPolicy.canAccess(entity)) {
            throw new AttachmentAccessDeniedException(id, "存取");
        }

        InputStream inputStream = storageStrategy.load(entity.getStoredFilename());
        return new AttachmentDownloadResponse(
                inputStream,
                entity.getOriginalFilename(),
                entity.getMimeType(),
                entity.getFileSize()
        );
    }

    @Transactional
    public void softDelete(Long id) {
        AttachmentEntity entity = findActiveOrThrow(id);
        if (!accessPolicy.canDelete(entity)) {
            throw new AttachmentAccessDeniedException(id, "刪除");
        }

        entity.delete();
        attachmentRepository.save(entity);
        log.info("附件已軟刪除: id={}, filename={}", id, entity.getOriginalFilename());

        eventPublisher.publishEvent(new AttachmentDeletedEvent(this, entity));
    }

    @Transactional(readOnly = true)
    public List<AttachmentUploadResponse> findByOwner(AttachmentOwnerRef ownerRef) {
        return attachmentRepository.findByOwner(ownerRef.ownerType(), ownerRef.ownerId())
                .stream()
                .map(this::toUploadResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttachmentUploadResponse findById(Long id) {
        return toUploadResponse(findActiveOrThrow(id));
    }

    private AttachmentEntity findActiveOrThrow(Long id) {
        return attachmentRepository.findByIdActive(id)
                .orElseThrow(() -> new AttachmentNotFoundException(id));
    }

    private AttachmentUploadResponse toUploadResponse(AttachmentEntity entity) {
        return new AttachmentUploadResponse(
                entity.getId(),
                entity.getOwnerType(),
                entity.getOwnerId(),
                entity.getOriginalFilename(),
                entity.getDisplayName(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getStorageType(),
                entity.getCreatedDate()
        );
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1).toLowerCase() : null;
    }
}
