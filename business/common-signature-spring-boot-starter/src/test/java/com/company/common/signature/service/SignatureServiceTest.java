package com.company.common.signature.service;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import com.company.common.attachment.storage.StorageType;
import com.company.common.signature.dto.SignatureResponse;
import com.company.common.signature.dto.SignatureSaveRequest;
import com.company.common.signature.entity.SignatureDiagram;
import com.company.common.signature.repository.SignatureDiagramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureServiceTest {

    @Mock
    private SignatureDiagramRepository repository;
    @Mock
    private AttachmentService attachmentService;

    private SignatureService signatureService;

    @BeforeEach
    void setUp() {
        signatureService = new SignatureService(repository, attachmentService);
    }

    private SignatureDiagram createDiagram(Long id, String ownerType, Long ownerId,
                                            String content, Long attachmentId) {
        SignatureDiagram d = new SignatureDiagram();
        d.setId(id);
        d.setOwnerType(ownerType);
        d.setOwnerId(ownerId);
        d.setContent(content);
        d.setAttachmentId(attachmentId);
        return d;
    }

    @Nested
    @DisplayName("save — 新增簽名")
    class SaveNew {

        @Test
        @DisplayName("第一次簽名 → 新建一筆")
        void firstSignature_createsNew() throws IOException {
            when(repository.findActiveByOwner("ORDER", 1L)).thenReturn(Optional.empty());
            when(repository.save(any(SignatureDiagram.class))).thenAnswer(inv -> {
                SignatureDiagram d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });

            SignatureResponse resp = signatureService.save(
                    new SignatureSaveRequest("ORDER", 1L, "{\"objects\":[]}"), null);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.ownerType()).isEqualTo("ORDER");
            assertThat(resp.content()).isEqualTo("{\"objects\":[]}");

            // 只存了一次（新建），沒有軟刪除
            verify(repository, times(1)).save(any());
            verify(attachmentService, never()).softDelete(any());
        }
    }

    @Nested
    @DisplayName("save — 重簽覆蓋")
    class SaveOverwrite {

        @Test
        @DisplayName("重簽 → 舊的整筆軟刪除 + 新建一筆")
        void resignature_softDeletesOldAndCreatesNew() throws IOException {
            SignatureDiagram old = createDiagram(1L, "ORDER", 1L, "{\"old\":true}", 100L);
            when(repository.findActiveByOwner("ORDER", 1L)).thenReturn(Optional.of(old));
            when(repository.save(any(SignatureDiagram.class))).thenAnswer(inv -> {
                SignatureDiagram d = inv.getArgument(0);
                if (d.getId() == null) {
                    d.setId(2L); // 新建的
                }
                return d;
            });

            SignatureResponse resp = signatureService.save(
                    new SignatureSaveRequest("ORDER", 1L, "{\"new\":true}"), null);

            // 舊的被軟刪除
            assertThat(old.isDeleted()).isTrue();

            // 舊附件被軟刪除
            verify(attachmentService).softDelete(100L);

            // 新建的 ID 不同
            assertThat(resp.id()).isEqualTo(2L);
            assertThat(resp.content()).isEqualTo("{\"new\":true}");

            // 存了兩次：1 次軟刪除舊的 + 1 次新建
            verify(repository, times(2)).save(any());
        }

        @Test
        @DisplayName("重簽 → 舊的 content 保留在舊筆")
        void resignature_oldContentPreserved() throws IOException {
            SignatureDiagram old = createDiagram(1L, "ORDER", 1L, "{\"old\":true}", null);
            when(repository.findActiveByOwner("ORDER", 1L)).thenReturn(Optional.of(old));
            when(repository.save(any(SignatureDiagram.class))).thenAnswer(inv -> inv.getArgument(0));

            signatureService.save(
                    new SignatureSaveRequest("ORDER", 1L, "{\"new\":true}"), null);

            // 舊的 content 沒被覆蓋
            assertThat(old.getContent()).isEqualTo("{\"old\":true}");
            assertThat(old.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("重簽 → 舊的無附件時不呼叫 softDelete")
        void resignature_noOldAttachment_skipsSoftDelete() throws IOException {
            SignatureDiagram old = createDiagram(1L, "ORDER", 1L, "{}", null);
            when(repository.findActiveByOwner("ORDER", 1L)).thenReturn(Optional.of(old));
            when(repository.save(any(SignatureDiagram.class))).thenAnswer(inv -> inv.getArgument(0));

            signatureService.save(
                    new SignatureSaveRequest("ORDER", 1L, "{\"new\":true}"), null);

            verify(attachmentService, never()).softDelete(any());
        }
    }

    @Nested
    @DisplayName("delete — 軟刪除")
    class Delete {

        @Test
        @DisplayName("刪除簽名 → 簽名 + 附件都軟刪除")
        void delete_softDeletesBoth() {
            SignatureDiagram diagram = createDiagram(1L, "ORDER", 1L, "{}", 100L);
            when(repository.findActiveByOwner("ORDER", 1L)).thenReturn(Optional.of(diagram));

            signatureService.delete("ORDER", 1L);

            assertThat(diagram.isDeleted()).isTrue();
            verify(repository).save(diagram);
            verify(attachmentService).softDelete(100L);
        }

        @Test
        @DisplayName("刪除不存在的簽名 → 不做任何事")
        void delete_notFound_noop() {
            when(repository.findActiveByOwner("ORDER", 999L)).thenReturn(Optional.empty());

            signatureService.delete("ORDER", 999L);

            verify(repository, never()).save(any());
            verify(attachmentService, never()).softDelete(any());
        }

        @Test
        @DisplayName("刪除後查詢 → 回傳 null")
        void delete_thenFind_returnsNull() {
            when(repository.findActiveByOwner("ORDER", 1L)).thenReturn(Optional.empty());

            SignatureResponse resp = signatureService.findByOwner("ORDER", 1L);

            assertThat(resp).isNull();
        }
    }

    @Nested
    @DisplayName("findByOwner — 查詢")
    class Find {

        @Test
        @DisplayName("查詢存在的簽名 → 回傳")
        void findExisting() {
            SignatureDiagram diagram = createDiagram(1L, "ORDER", 1L, "{\"objects\":[]}", 100L);
            when(repository.findActiveByOwner("ORDER", 1L)).thenReturn(Optional.of(diagram));

            SignatureResponse resp = signatureService.findByOwner("ORDER", 1L);

            assertThat(resp).isNotNull();
            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.content()).isEqualTo("{\"objects\":[]}");
        }

        @Test
        @DisplayName("查詢不存在 → null")
        void findNotExisting() {
            when(repository.findActiveByOwner("ORDER", 999L)).thenReturn(Optional.empty());

            assertThat(signatureService.findByOwner("ORDER", 999L)).isNull();
        }
    }
}
