package com.company.common.attachment.validation;

import com.company.common.attachment.config.AttachmentProperties;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FileSizeValidatorTest {

    private FileSizeValidator validator;

    @BeforeEach
    void setUp() {
        AttachmentProperties properties = new AttachmentProperties();
        properties.setMaxFileSize(DataSize.ofMegabytes(10));
        validator = new FileSizeValidator(properties);
    }

    @Test
    @DisplayName("檔案大小在限制內應通過")
    void fileWithinLimit_shouldPass() {
        AttachmentUploadRequest request = createRequest(1024 * 1024); // 1MB
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("檔案超過大小限制應被拒絕")
    void fileExceedsLimit_shouldReject() {
        AttachmentUploadRequest request = createRequest(20 * 1024 * 1024); // 20MB
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(AttachmentValidationException.class)
                .hasMessageContaining("超過上限");
    }

    private AttachmentUploadRequest createRequest(long fileSize) {
        return new AttachmentUploadRequest(
                "TEST", 1L, "test.pdf", null,
                new ByteArrayInputStream(new byte[0]), fileSize, "application/pdf"
        );
    }
}
