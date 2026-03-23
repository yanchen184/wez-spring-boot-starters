package com.company.common.diagram.service;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import com.company.common.attachment.storage.StorageType;
import com.company.common.diagram.dto.DiagramResponse;
import com.company.common.diagram.dto.DiagramSaveRequest;
import com.company.common.diagram.entity.DiagramEntity;
import com.company.common.diagram.repository.DiagramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagramServiceTest {

    @Mock
    private DiagramRepository repository;
    @Mock
    private AttachmentService attachmentService;

    private DiagramService diagramService;

    @BeforeEach
    void setUp() {
        diagramService = new DiagramService(repository, attachmentService);
    }

    private DiagramEntity createDiagram(Long id, String diagramType, String ownerType,
                                         Long ownerId, String content, Long attachmentId) {
        DiagramEntity d = new DiagramEntity();
        d.setId(id);
        d.setDiagramType(diagramType);
        d.setOwnerType(ownerType);
        d.setOwnerId(ownerId);
        d.setContent(content);
        d.setAttachmentId(attachmentId);
        return d;
    }

    private DiagramEntity createDiagram(Long id, String diagramType, String ownerType,
                                         Long ownerId, String name, String content, Long attachmentId) {
        DiagramEntity d = createDiagram(id, diagramType, ownerType, ownerId, content, attachmentId);
        d.setName(name);
        return d;
    }

    @Nested
    @DisplayName("save - 新增圖表")
    class SaveNew {

        @Test
        @DisplayName("第一次建立 SIGNATURE -> 新建一筆")
        void firstDiagram_createsNew() throws IOException {
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 1L)).thenReturn(Optional.empty());
            when(repository.save(any(DiagramEntity.class))).thenAnswer(inv -> {
                DiagramEntity d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });

            DiagramResponse resp = diagramService.save(
                    new DiagramSaveRequest("SIGNATURE", "ORDER", 1L, null, "{\"objects\":[]}"), null);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.diagramType()).isEqualTo("SIGNATURE");
            assertThat(resp.ownerType()).isEqualTo("ORDER");
            assertThat(resp.content()).isEqualTo("{\"objects\":[]}");

            verify(repository, times(1)).save(any());
            verify(attachmentService, never()).softDelete(any());
        }

        @Test
        @DisplayName("第一次建立 FAMILY -> 新建一筆（含 name）")
        void firstFamilyDiagram_createsNewWithName() throws IOException {
            when(repository.findActiveDiagram("FAMILY", "CASE", 1L)).thenReturn(Optional.empty());
            when(repository.save(any(DiagramEntity.class))).thenAnswer(inv -> {
                DiagramEntity d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });

            DiagramResponse resp = diagramService.save(
                    new DiagramSaveRequest("FAMILY", "CASE", 1L, "v1", "{\"nodes\":[]}"), null);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.diagramType()).isEqualTo("FAMILY");
            assertThat(resp.name()).isEqualTo("v1");
        }
    }

    @Nested
    @DisplayName("save - 重新建立（覆蓋）")
    class SaveOverwrite {

        @Test
        @DisplayName("重簽 -> 舊的整筆軟刪除 + 新建一筆")
        void overwrite_softDeletesOldAndCreatesNew() throws IOException {
            DiagramEntity old = createDiagram(1L, "SIGNATURE", "ORDER", 1L, "{\"old\":true}", 100L);
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 1L)).thenReturn(Optional.of(old));
            when(repository.save(any(DiagramEntity.class))).thenAnswer(inv -> {
                DiagramEntity d = inv.getArgument(0);
                if (d.getId() == null) {
                    d.setId(2L);
                }
                return d;
            });

            DiagramResponse resp = diagramService.save(
                    new DiagramSaveRequest("SIGNATURE", "ORDER", 1L, null, "{\"new\":true}"), null);

            assertThat(old.isDeleted()).isTrue();
            verify(attachmentService).softDelete(100L);
            assertThat(resp.id()).isEqualTo(2L);
            assertThat(resp.content()).isEqualTo("{\"new\":true}");
            verify(repository, times(2)).save(any());
        }

        @Test
        @DisplayName("重簽 -> 舊的 content 保留在舊筆")
        void overwrite_oldContentPreserved() throws IOException {
            DiagramEntity old = createDiagram(1L, "SIGNATURE", "ORDER", 1L, "{\"old\":true}", null);
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 1L)).thenReturn(Optional.of(old));
            when(repository.save(any(DiagramEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            diagramService.save(
                    new DiagramSaveRequest("SIGNATURE", "ORDER", 1L, null, "{\"new\":true}"), null);

            assertThat(old.getContent()).isEqualTo("{\"old\":true}");
            assertThat(old.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("重簽 -> 舊的無附件時不呼叫 softDelete")
        void overwrite_noOldAttachment_skipsSoftDelete() throws IOException {
            DiagramEntity old = createDiagram(1L, "SIGNATURE", "ORDER", 1L, "{}", null);
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 1L)).thenReturn(Optional.of(old));
            when(repository.save(any(DiagramEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            diagramService.save(
                    new DiagramSaveRequest("SIGNATURE", "ORDER", 1L, null, "{\"new\":true}"), null);

            verify(attachmentService, never()).softDelete(any());
        }
    }

    @Nested
    @DisplayName("diagramType 過濾")
    class DiagramTypeFiltering {

        @Test
        @DisplayName("不同 diagramType 的查詢互不干擾")
        void differentTypes_independent() {
            DiagramEntity sig = createDiagram(1L, "SIGNATURE", "ORDER", 1L, "{\"sig\":true}", null);
            DiagramEntity fam = createDiagram(2L, "FAMILY", "ORDER", 1L, "{\"fam\":true}", null);

            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 1L)).thenReturn(Optional.of(sig));
            when(repository.findActiveDiagram("FAMILY", "ORDER", 1L)).thenReturn(Optional.of(fam));

            DiagramResponse sigResp = diagramService.findByOwner("SIGNATURE", "ORDER", 1L);
            DiagramResponse famResp = diagramService.findByOwner("FAMILY", "ORDER", 1L);

            assertThat(sigResp.diagramType()).isEqualTo("SIGNATURE");
            assertThat(famResp.diagramType()).isEqualTo("FAMILY");
            assertThat(sigResp.id()).isNotEqualTo(famResp.id());
        }
    }

    @Nested
    @DisplayName("delete - 軟刪除")
    class Delete {

        @Test
        @DisplayName("刪除圖表 -> 圖表 + 附件都軟刪除")
        void delete_softDeletesBoth() {
            DiagramEntity diagram = createDiagram(1L, "SIGNATURE", "ORDER", 1L, "{}", 100L);
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 1L)).thenReturn(Optional.of(diagram));

            diagramService.delete("SIGNATURE", "ORDER", 1L);

            assertThat(diagram.isDeleted()).isTrue();
            verify(repository).save(diagram);
            verify(attachmentService).softDelete(100L);
        }

        @Test
        @DisplayName("刪除不存在的圖表 -> 不做任何事")
        void delete_notFound_noop() {
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 999L)).thenReturn(Optional.empty());

            diagramService.delete("SIGNATURE", "ORDER", 999L);

            verify(repository, never()).save(any());
            verify(attachmentService, never()).softDelete(any());
        }

        @Test
        @DisplayName("刪除後查詢 -> 回傳 null")
        void delete_thenFind_returnsNull() {
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 1L)).thenReturn(Optional.empty());

            DiagramResponse resp = diagramService.findByOwner("SIGNATURE", "ORDER", 1L);

            assertThat(resp).isNull();
        }
    }

    @Nested
    @DisplayName("findByOwner - 查詢")
    class Find {

        @Test
        @DisplayName("查詢存在的圖表 -> 回傳")
        void findExisting() {
            DiagramEntity diagram = createDiagram(1L, "SIGNATURE", "ORDER", 1L, "{\"objects\":[]}", 100L);
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 1L)).thenReturn(Optional.of(diagram));

            DiagramResponse resp = diagramService.findByOwner("SIGNATURE", "ORDER", 1L);

            assertThat(resp).isNotNull();
            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.content()).isEqualTo("{\"objects\":[]}");
        }

        @Test
        @DisplayName("查詢不存在 -> null")
        void findNotExisting() {
            when(repository.findActiveDiagram("SIGNATURE", "ORDER", 999L)).thenReturn(Optional.empty());

            assertThat(diagramService.findByOwner("SIGNATURE", "ORDER", 999L)).isNull();
        }
    }

    @Nested
    @DisplayName("getHistory - 版本歷史")
    class History {

        @Test
        @DisplayName("歷史記錄包含已刪除的版本")
        void history_includesDeleted() {
            DiagramEntity v1 = createDiagram(1L, "FAMILY", "CASE", 1L, "v1", "{\"v1\":true}", null);
            v1.delete();
            DiagramEntity v2 = createDiagram(2L, "FAMILY", "CASE", 1L, "v2", "{\"v2\":true}", null);

            when(repository.findAllVersions("FAMILY", "CASE", 1L)).thenReturn(List.of(v2, v1));

            List<DiagramResponse> history = diagramService.getHistory("FAMILY", "CASE", 1L);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).deleted()).isFalse();
            assertThat(history.get(1).deleted()).isTrue();
            assertThat(history.get(1).name()).isEqualTo("v1");
        }

        @Test
        @DisplayName("無歷史記錄 -> 空 list")
        void history_empty() {
            when(repository.findAllVersions("FAMILY", "CASE", 999L)).thenReturn(List.of());

            List<DiagramResponse> history = diagramService.getHistory("FAMILY", "CASE", 999L);

            assertThat(history).isEmpty();
        }
    }

    @Nested
    @DisplayName("restoreVersion - 還原版本")
    class RestoreVersion {

        @Test
        @DisplayName("還原已刪除的版本 -> 軟刪除目前 active + 還原目標")
        void restore_softDeletesCurrentAndRestoresTarget() {
            DiagramEntity deleted = createDiagram(1L, "FAMILY", "CASE", 1L, "v1", "{\"v1\":true}", null);
            deleted.delete();
            DiagramEntity current = createDiagram(2L, "FAMILY", "CASE", 1L, "v2", "{\"v2\":true}", null);

            when(repository.findById(1L)).thenReturn(Optional.of(deleted));
            when(repository.findActiveDiagram("FAMILY", "CASE", 1L)).thenReturn(Optional.of(current));
            when(repository.save(any(DiagramEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            DiagramResponse resp = diagramService.restoreVersion(1L);

            assertThat(current.isDeleted()).isTrue();
            assertThat(deleted.isDeleted()).isFalse();
            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.name()).isEqualTo("v1");
            // save: 1 次 current 軟刪除 + 1 次 target 還原
            verify(repository, times(2)).save(any());
        }

        @Test
        @DisplayName("還原已經 active 的版本 -> 直接回傳，不做任何變更")
        void restore_alreadyActive_noop() {
            DiagramEntity active = createDiagram(1L, "FAMILY", "CASE", 1L, "v1", "{\"v1\":true}", null);

            when(repository.findById(1L)).thenReturn(Optional.of(active));

            DiagramResponse resp = diagramService.restoreVersion(1L);

            assertThat(resp.id()).isEqualTo(1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("還原不存在的 ID -> 拋出例外")
        void restore_notFound_throws() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diagramService.restoreVersion(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }
}
