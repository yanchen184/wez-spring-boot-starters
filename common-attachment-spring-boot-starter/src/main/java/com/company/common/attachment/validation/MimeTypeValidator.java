package com.company.common.attachment.validation;

import com.company.common.attachment.config.AttachmentProperties;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class MimeTypeValidator implements AttachmentValidator {

    private final AttachmentProperties properties;

    @Override
    public void validate(AttachmentUploadRequest request) {
        String mimeType = request.contentType();
        Set<String> allowedTypes = properties.getAllowedMimeTypes();

        // 如果未設定允許清單，表示不限制 MIME type（但仍會由副檔名 blocklist 把關）
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            log.debug("未設定 MIME type 允許清單，跳過 MIME type 驗證");
            return;
        }

        if (!allowedTypes.contains(mimeType)) {
            throw new AttachmentValidationException(
                    String.format("不允許的 MIME type: %s（允許: %s）", mimeType, allowedTypes)
            );
        }
        log.debug("MIME type 驗證通過: {}", mimeType);
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
