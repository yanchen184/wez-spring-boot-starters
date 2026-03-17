package com.company.common.attachment.validation;

import com.company.common.attachment.core.model.AttachmentUploadRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PathTraversalGuard implements AttachmentValidator {

    @Override
    public void validate(AttachmentUploadRequest request) {
        String filename = request.originalFilename();
        if (filename == null || filename.isBlank()) {
            throw new AttachmentValidationException("檔案名稱不得為空");
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new AttachmentValidationException(
                    "檔案名稱包含不合法字元（路徑穿越風險）: " + filename
            );
        }

        // 檢查 null bytes
        if (filename.indexOf('\0') >= 0) {
            throw new AttachmentValidationException("檔案名稱包含 null byte");
        }

        // 副檔名 blocklist
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".exe") || lowerFilename.endsWith(".bat")
                || lowerFilename.endsWith(".cmd") || lowerFilename.endsWith(".sh")
                || lowerFilename.endsWith(".ps1") || lowerFilename.endsWith(".vbs")
                || lowerFilename.endsWith(".js") || lowerFilename.endsWith(".jar")
                || lowerFilename.endsWith(".war") || lowerFilename.endsWith(".class")) {
            throw new AttachmentValidationException("不允許的副檔名: " + filename);
        }

        log.debug("路徑穿越與副檔名驗證通過: {}", filename);
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
