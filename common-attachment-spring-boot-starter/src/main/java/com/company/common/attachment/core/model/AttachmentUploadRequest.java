package com.company.common.attachment.core.model;

import java.io.InputStream;

public record AttachmentUploadRequest(
        String ownerType,
        Long ownerId,
        String originalFilename,
        String displayName,
        InputStream inputStream,
        long fileSize,
        String contentType
) {
}
