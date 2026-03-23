package com.company.common.diagram.service;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.attachment.core.model.AttachmentOwnerRef;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import com.company.common.diagram.dto.DiagramResponse;
import com.company.common.diagram.dto.DiagramSaveRequest;
import com.company.common.diagram.entity.DiagramEntity;
import com.company.common.diagram.repository.DiagramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DiagramService {

    private static final String ATTACHMENT_OWNER_PREFIX = "DIAGRAM_";

    private final DiagramRepository repository;
    private final AttachmentService attachmentService;

    /**
     * 儲存圖表：軟刪除舊的 active 版本，新建一筆。
     */
    @Transactional
    public DiagramResponse save(DiagramSaveRequest request, MultipartFile image) throws IOException {
        repository.findActiveDiagram(request.diagramType(), request.ownerType(), request.ownerId())
                .ifPresent(old -> {
                    if (old.getAttachmentId() != null) {
                        try {
                            attachmentService.softDelete(old.getAttachmentId());
                        } catch (Exception e) {
                            log.warn("軟刪除舊圖表附件失敗: attachmentId={}", old.getAttachmentId(), e);
                        }
                    }
                    old.delete();
                    repository.save(old);
                    log.info("舊圖表已軟刪除: id={}, diagramType={}, ownerType={}, ownerId={}",
                            old.getId(), old.getDiagramType(), old.getOwnerType(), old.getOwnerId());
                });

        DiagramEntity diagram = new DiagramEntity();
        diagram.setDiagramType(request.diagramType());
        diagram.setOwnerType(request.ownerType());
        diagram.setOwnerId(request.ownerId());
        diagram.setName(request.name());
        diagram.setContent(request.json());

        if (image != null && !image.isEmpty()) {
            String attachOwnerType = buildAttachmentOwnerType(request.diagramType(), request.ownerType());
            AttachmentUploadRequest uploadRequest = new AttachmentUploadRequest(
                    attachOwnerType,
                    request.ownerId(),
                    image.getOriginalFilename(),
                    request.diagramType() + " 圖片",
                    image.getInputStream(),
                    image.getSize(),
                    image.getContentType()
            );
            AttachmentUploadResponse uploadResponse = attachmentService.upload(uploadRequest);
            diagram.setAttachmentId(uploadResponse.id());
        }

        diagram = repository.save(diagram);
        log.info("圖表儲存完成: id={}, diagramType={}, ownerType={}, ownerId={}",
                diagram.getId(), diagram.getDiagramType(), diagram.getOwnerType(), diagram.getOwnerId());

        return toResponse(diagram);
    }

    @Transactional(readOnly = true)
    public DiagramResponse findByOwner(String diagramType, String ownerType, Long ownerId) {
        return repository.findActiveDiagram(diagramType, ownerType, ownerId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DiagramResponse> getHistory(String diagramType, String ownerType, Long ownerId) {
        return repository.findAllVersions(diagramType, ownerType, ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String diagramType, String ownerType, Long ownerId) {
        repository.findActiveDiagram(diagramType, ownerType, ownerId)
                .ifPresent(diagram -> {
                    if (diagram.getAttachmentId() != null) {
                        try {
                            attachmentService.softDelete(diagram.getAttachmentId());
                        } catch (Exception e) {
                            log.warn("軟刪除圖表附件失敗: attachmentId={}", diagram.getAttachmentId(), e);
                        }
                    }
                    diagram.delete();
                    repository.save(diagram);
                    log.info("圖表已軟刪除: diagramType={}, ownerType={}, ownerId={}",
                            diagramType, ownerType, ownerId);
                });
    }

    /**
     * 還原指定版本：軟刪除目前 active 版本，還原目標版本。
     */
    @Transactional
    public DiagramResponse restoreVersion(Long id) {
        DiagramEntity target = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到圖表版本: id=" + id));

        if (!target.isDeleted()) {
            return toResponse(target);
        }

        // 軟刪除目前 active 版本
        repository.findActiveDiagram(target.getDiagramType(), target.getOwnerType(), target.getOwnerId())
                .ifPresent(current -> {
                    current.delete();
                    repository.save(current);
                    log.info("還原版本時軟刪除目前 active: id={}", current.getId());
                });

        target.restore();
        target = repository.save(target);
        log.info("圖表版本已還原: id={}, diagramType={}, ownerType={}, ownerId={}",
                target.getId(), target.getDiagramType(), target.getOwnerType(), target.getOwnerId());

        return toResponse(target);
    }

    @Transactional(readOnly = true)
    public List<AttachmentUploadResponse> getAttachments(String diagramType, String ownerType, Long ownerId) {
        String attachOwnerType = buildAttachmentOwnerType(diagramType, ownerType);
        return attachmentService.findByOwner(new AttachmentOwnerRef(attachOwnerType, ownerId));
    }

    private String buildAttachmentOwnerType(String diagramType, String ownerType) {
        return ATTACHMENT_OWNER_PREFIX + diagramType + "_" + ownerType;
    }

    private DiagramResponse toResponse(DiagramEntity diagram) {
        return new DiagramResponse(
                diagram.getId(),
                diagram.getDiagramType(),
                diagram.getOwnerType(),
                diagram.getOwnerId(),
                diagram.getName(),
                diagram.getContent(),
                diagram.getAttachmentId(),
                diagram.isDeleted(),
                diagram.getCreatedBy(),
                diagram.getCreatedDate(),
                diagram.getLastModifiedBy(),
                diagram.getLastModifiedDate()
        );
    }
}
