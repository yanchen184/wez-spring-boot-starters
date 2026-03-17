package com.company.common.attachment.core.model;

import java.io.InputStream;

public record AttachmentDownloadResponse(
        InputStream inputStream,
        String originalFilename,
        String mimeType,
        Long fileSize
) {
}
