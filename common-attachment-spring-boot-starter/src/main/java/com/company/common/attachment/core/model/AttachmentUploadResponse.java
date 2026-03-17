package com.company.common.attachment.core.model;

import com.company.common.attachment.storage.StorageType;

import java.time.LocalDateTime;

public record AttachmentUploadResponse(
        Long id,
        String ownerType,
        Long ownerId,
        String originalFilename,
        String displayName,
        String mimeType,
        Long fileSize,
        StorageType storageType,
        LocalDateTime createdDate
) {
}
