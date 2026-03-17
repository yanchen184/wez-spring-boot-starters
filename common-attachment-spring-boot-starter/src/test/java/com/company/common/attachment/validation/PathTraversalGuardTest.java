package com.company.common.attachment.validation;

import com.company.common.attachment.core.model.AttachmentUploadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PathTraversalGuardTest {

    private PathTraversalGuard guard;

    @BeforeEach
    void setUp() {
        guard = new PathTraversalGuard();
    }

    @Test
    @DisplayName("合法檔名應通過驗證")
    void validFilename_shouldPass() {
        AttachmentUploadRequest request = createRequest("test-document.pdf");
        assertDoesNotThrow(() -> guard.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../etc/passwd", "..\\windows\\system32", "foo/../bar.txt"})
    @DisplayName("路徑穿越攻擊應被拒絕")
    void pathTraversal_shouldReject(String filename) {
        AttachmentUploadRequest request = createRequest(filename);
        assertThatThrownBy(() -> guard.validate(request))
                .isInstanceOf(AttachmentValidationException.class)
                .hasMessageContaining("路徑穿越");
    }

    @ParameterizedTest
    @ValueSource(strings = {"malware.exe", "script.bat", "hack.sh", "evil.jar"})
    @DisplayName("危險副檔名應被拒絕")
    void dangerousExtension_shouldReject(String filename) {
        AttachmentUploadRequest request = createRequest(filename);
        assertThatThrownBy(() -> guard.validate(request))
                .isInstanceOf(AttachmentValidationException.class)
                .hasMessageContaining("不允許的副檔名");
    }

    @Test
    @DisplayName("空檔名應被拒絕")
    void nullFilename_shouldReject() {
        AttachmentUploadRequest request = createRequest(null);
        assertThatThrownBy(() -> guard.validate(request))
                .isInstanceOf(AttachmentValidationException.class);
    }

    private AttachmentUploadRequest createRequest(String filename) {
        return new AttachmentUploadRequest(
                "TEST", 1L, filename, null,
                new ByteArrayInputStream(new byte[0]), 0, "application/octet-stream"
        );
    }
}
