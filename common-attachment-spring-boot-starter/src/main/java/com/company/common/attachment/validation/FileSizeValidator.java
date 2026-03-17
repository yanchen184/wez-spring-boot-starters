package com.company.common.attachment.validation;

import com.company.common.attachment.config.AttachmentProperties;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FileSizeValidator implements AttachmentValidator {

    private final AttachmentProperties properties;

    @Override
    public void validate(AttachmentUploadRequest request) {
        long maxBytes = properties.getMaxFileSize().toBytes();
        if (request.fileSize() > maxBytes) {
            throw new AttachmentValidationException(
                    String.format("檔案大小 %d bytes 超過上限 %d bytes", request.fileSize(), maxBytes)
            );
        }
        log.debug("檔案大小驗證通過: {} bytes (上限 {} bytes)", request.fileSize(), maxBytes);
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
