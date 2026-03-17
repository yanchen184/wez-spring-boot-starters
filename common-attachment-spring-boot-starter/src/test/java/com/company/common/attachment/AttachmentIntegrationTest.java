package com.company.common.attachment;

import com.company.common.attachment.core.AttachmentNotFoundException;
import com.company.common.attachment.core.AttachmentService;
import com.company.common.attachment.core.model.AttachmentDownloadResponse;
import com.company.common.attachment.core.model.AttachmentOwnerRef;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import com.company.common.attachment.persistence.entity.AttachmentEntity;
import com.company.common.attachment.security.AttachmentAccessPolicy;
import com.company.common.attachment.security.DefaultDenyAccessPolicy;
import com.company.common.attachment.storage.StorageType;
import com.company.common.attachment.validation.AttachmentValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 整合測試 — H2 + 檔案系統，走完整個上傳→查詢→下載→刪除流程
 */
@SpringBootTest(classes = TestApplication.class)
@Import(AttachmentIntegrationTest.TestConfig.class)
class AttachmentIntegrationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("wez.attachment.storage-path", () -> tempDir.toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:attachment-test;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        AttachmentAccessPolicy allowAllPolicy() {
            return new AttachmentAccessPolicy() {
                @Override
                public boolean canAccess(AttachmentEntity attachment) {
                    return true;
                }

                @Override
                public boolean canDelete(AttachmentEntity attachment) {
                    return true;
                }
            };
        }
    }

    @Autowired
    private AttachmentService attachmentService;

    // ===== 上傳 =====

    @Nested
    @DisplayName("上傳")
    class Upload {

        @Test
        @DisplayName("上傳 PDF → 應成功儲存並回傳 metadata")
        void uploadPdf() throws IOException {
            byte[] content = "%PDF-1.4 fake pdf content".getBytes();
            AttachmentUploadRequest request = new AttachmentUploadRequest(
                    "ORDER", 100L, "invoice.pdf", "發票.pdf",
                    new ByteArrayInputStream(content), content.length, "application/pdf"
            );

            AttachmentUploadResponse response = attachmentService.upload(request);

            assertThat(response.id()).isNotNull();
            assertThat(response.ownerType()).isEqualTo("ORDER");
            assertThat(response.ownerId()).isEqualTo(100L);
            assertThat(response.originalFilename()).isEqualTo("invoice.pdf");
            assertThat(response.displayName()).isEqualTo("發票.pdf");
            assertThat(response.fileSize()).isEqualTo(content.length);
            assertThat(response.storageType()).isEqualTo(StorageType.FILESYSTEM);
        }

        @Test
        @DisplayName("上傳純文字檔 → displayName null 時用 originalFilename")
        void uploadText() throws IOException {
            byte[] content = "Hello, World!".getBytes();
            AttachmentUploadRequest request = new AttachmentUploadRequest(
                    "ORDER", 100L, "readme.txt", null,
                    new ByteArrayInputStream(content), content.length, "text/plain"
            );

            AttachmentUploadResponse response = attachmentService.upload(request);

            assertThat(response.id()).isNotNull();
            assertThat(response.displayName()).isEqualTo("readme.txt");
        }

        @Test
        @DisplayName("上傳超過大小限制 → 應拋出 AttachmentValidationException")
        void uploadTooLarge() {
            byte[] content = new byte[60 * 1024 * 1024]; // 60MB
            AttachmentUploadRequest request = new AttachmentUploadRequest(
                    "ORDER", 1L, "huge.bin", null,
                    new ByteArrayInputStream(content), content.length, "application/octet-stream"
            );

            assertThatThrownBy(() -> attachmentService.upload(request))
                    .isInstanceOf(AttachmentValidationException.class);
        }
    }

    // ===== 查詢 =====

    @Nested
    @DisplayName("查詢")
    class Find {

        @Test
        @DisplayName("依 owner 查詢 → 應回傳該 owner 的附件")
        void findByOwner() throws IOException {
            for (int i = 0; i < 2; i++) {
                byte[] content = ("file-" + i).getBytes();
                attachmentService.upload(new AttachmentUploadRequest(
                        "CUSTOMER", 200L, "doc" + i + ".txt", null,
                        new ByteArrayInputStream(content), content.length, "text/plain"
                ));
            }

            List<AttachmentUploadResponse> result = attachmentService.findByOwner(
                    new AttachmentOwnerRef("CUSTOMER", 200L));

            assertThat(result).hasSizeGreaterThanOrEqualTo(2);
            assertThat(result).allMatch(r -> "CUSTOMER".equals(r.ownerType()));
        }

        @Test
        @DisplayName("查詢不存在的 ID → 應拋出 AttachmentNotFoundException")
        void findByIdNotFound() {
            assertThatThrownBy(() -> attachmentService.findById(99999L))
                    .isInstanceOf(AttachmentNotFoundException.class);
        }
    }

    // ===== 下載 =====

    @Nested
    @DisplayName("下載")
    class Download {

        @Test
        @DisplayName("上傳後下載 → 內容應一致")
        void downloadContentShouldMatch() throws IOException {
            byte[] originalContent = "download test content 123".getBytes();
            AttachmentUploadResponse uploaded = attachmentService.upload(new AttachmentUploadRequest(
                    "REPORT", 300L, "data.txt", null,
                    new ByteArrayInputStream(originalContent), originalContent.length, "text/plain"
            ));

            AttachmentDownloadResponse downloaded = attachmentService.download(uploaded.id());

            assertThat(downloaded.originalFilename()).isEqualTo("data.txt");
            assertThat(downloaded.fileSize()).isEqualTo(originalContent.length);

            byte[] downloadedContent = downloaded.inputStream().readAllBytes();
            assertThat(downloadedContent).isEqualTo(originalContent);
        }

        @Test
        @DisplayName("下載不存在的附件 → 應拋出 AttachmentNotFoundException")
        void downloadNotFound() {
            assertThatThrownBy(() -> attachmentService.download(99999L))
                    .isInstanceOf(AttachmentNotFoundException.class);
        }
    }

    // ===== 軟刪除 =====

    @Nested
    @DisplayName("軟刪除")
    class SoftDelete {

        @Test
        @DisplayName("刪除後查詢 → 應找不到")
        void deleteAndFindShouldFail() throws IOException {
            byte[] content = "will be deleted".getBytes();
            AttachmentUploadResponse uploaded = attachmentService.upload(new AttachmentUploadRequest(
                    "TEMP", 400L, "temp.txt", null,
                    new ByteArrayInputStream(content), content.length, "text/plain"
            ));

            attachmentService.softDelete(uploaded.id());

            assertThatThrownBy(() -> attachmentService.findById(uploaded.id()))
                    .isInstanceOf(AttachmentNotFoundException.class);
        }

        @Test
        @DisplayName("刪除不存在的附件 → 應拋出 AttachmentNotFoundException")
        void deleteNotFound() {
            assertThatThrownBy(() -> attachmentService.softDelete(99999L))
                    .isInstanceOf(AttachmentNotFoundException.class);
        }
    }

    // ===== 存取控制 =====

    @Nested
    @DisplayName("存取控制")
    class AccessControl {

        @Test
        @DisplayName("DefaultDenyAccessPolicy 應拒絕所有存取")
        void defaultDenyShouldRejectAll() {
            DefaultDenyAccessPolicy denyPolicy = new DefaultDenyAccessPolicy();
            AttachmentEntity entity = new AttachmentEntity();
            entity.setId(1L);

            assertThat(denyPolicy.canAccess(entity)).isFalse();
            assertThat(denyPolicy.canDelete(entity)).isFalse();
        }
    }
}
