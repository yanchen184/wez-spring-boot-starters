package com.company.common.attachment.core;

import com.company.common.attachment.config.AttachmentProperties;
import com.company.common.attachment.core.model.AttachmentDownloadResponse;
import com.company.common.attachment.core.model.AttachmentOwnerRef;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import com.company.common.attachment.persistence.entity.AttachmentEntity;
import com.company.common.attachment.persistence.repository.AttachmentRepository;
import com.company.common.attachment.security.AttachmentAccessPolicy;
import com.company.common.attachment.storage.AttachmentStorageStrategy;
import com.company.common.attachment.storage.StorageResult;
import com.company.common.attachment.storage.StorageType;
import com.company.common.attachment.validation.AttachmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AttachmentStorageStrategy storageStrategy;
    @Mock
    private AttachmentAccessPolicy accessPolicy;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        List<AttachmentValidator> validators = List.of();
        AttachmentProperties properties = new AttachmentProperties();
        attachmentService = new AttachmentService(
                attachmentRepository, storageStrategy, accessPolicy,
                validators, eventPublisher, properties);
    }

    @Test
    @DisplayName("上傳應儲存檔案並持久化 metadata")
    void upload_shouldStoreAndPersist() throws IOException {
        byte[] content = "test content".getBytes();
        AttachmentUploadRequest request = new AttachmentUploadRequest(
                "ORDER", 1L, "invoice.pdf", "發票",
                new ByteArrayInputStream(content), content.length, "application/pdf"
        );

        when(storageStrategy.store(anyString(), any(InputStream.class)))
                .thenReturn(new StorageResult("stored-uuid.pdf", StorageType.FILESYSTEM, content.length));

        AttachmentEntity savedEntity = new AttachmentEntity();
        savedEntity.setId(1L);
        savedEntity.setOwnerType("ORDER");
        savedEntity.setOwnerId(1L);
        savedEntity.setOriginalFilename("invoice.pdf");
        savedEntity.setStoredFilename("stored-uuid.pdf");
        savedEntity.setMimeType("application/pdf");
        savedEntity.setFileSize((long) content.length);
        savedEntity.setStorageType(StorageType.FILESYSTEM);
        savedEntity.setDisplayName("發票");

        when(attachmentRepository.save(any(AttachmentEntity.class))).thenReturn(savedEntity);

        AttachmentUploadResponse response = attachmentService.upload(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.ownerType()).isEqualTo("ORDER");
        assertThat(response.originalFilename()).isEqualTo("invoice.pdf");

        ArgumentCaptor<AttachmentEntity> captor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerType()).isEqualTo("ORDER");
    }

    @Test
    @DisplayName("下載不存在的附件應拋出 AttachmentNotFoundException")
    void download_notFound_shouldThrow() {
        when(attachmentRepository.findByIdActive(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.download(999L))
                .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    @DisplayName("存取被拒絕時應拋出 AttachmentAccessDeniedException")
    void download_accessDenied_shouldThrow() {
        AttachmentEntity entity = new AttachmentEntity();
        entity.setId(1L);
        when(attachmentRepository.findByIdActive(1L)).thenReturn(Optional.of(entity));
        when(accessPolicy.canAccess(entity)).thenReturn(false);

        assertThatThrownBy(() -> attachmentService.download(1L))
                .isInstanceOf(AttachmentAccessDeniedException.class);
    }

    @Test
    @DisplayName("下載應回傳 InputStream 與 metadata")
    void download_shouldReturnStream() throws IOException {
        AttachmentEntity entity = new AttachmentEntity();
        entity.setId(1L);
        entity.setOriginalFilename("test.pdf");
        entity.setMimeType("application/pdf");
        entity.setFileSize(100L);
        entity.setStoredFilename("stored.pdf");

        when(attachmentRepository.findByIdActive(1L)).thenReturn(Optional.of(entity));
        when(accessPolicy.canAccess(entity)).thenReturn(true);
        when(storageStrategy.load("stored.pdf"))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        AttachmentDownloadResponse response = attachmentService.download(1L);

        assertThat(response.originalFilename()).isEqualTo("test.pdf");
        assertThat(response.mimeType()).isEqualTo("application/pdf");
        assertThat(response.fileSize()).isEqualTo(100L);
        assertThat(response.inputStream()).isNotNull();
    }

    @Test
    @DisplayName("軟刪除應標記 deleted 並發布事件")
    void softDelete_shouldMarkAndPublish() {
        AttachmentEntity entity = new AttachmentEntity();
        entity.setId(1L);
        entity.setOriginalFilename("test.pdf");

        when(attachmentRepository.findByIdActive(1L)).thenReturn(Optional.of(entity));
        when(accessPolicy.canDelete(entity)).thenReturn(true);

        attachmentService.softDelete(1L);

        assertThat(entity.isDeleted()).isTrue();
        verify(attachmentRepository).save(entity);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("findByOwner 應回傳該 owner 的附件清單")
    void findByOwner_shouldReturnList() {
        AttachmentEntity entity = new AttachmentEntity();
        entity.setId(1L);
        entity.setOwnerType("ORDER");
        entity.setOwnerId(10L);
        entity.setOriginalFilename("test.pdf");
        entity.setMimeType("application/pdf");
        entity.setFileSize(100L);
        entity.setStorageType(StorageType.FILESYSTEM);

        when(attachmentRepository.findByOwner("ORDER", 10L)).thenReturn(List.of(entity));

        List<AttachmentUploadResponse> result = attachmentService.findByOwner(
                new AttachmentOwnerRef("ORDER", 10L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().ownerType()).isEqualTo("ORDER");
    }
}
