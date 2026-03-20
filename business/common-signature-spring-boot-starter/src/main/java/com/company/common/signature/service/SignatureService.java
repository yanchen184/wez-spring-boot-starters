package com.company.common.signature.service;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.attachment.core.model.AttachmentOwnerRef;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import com.company.common.signature.dto.SignatureResponse;
import com.company.common.signature.dto.SignatureSaveRequest;
import com.company.common.signature.entity.SignatureDiagram;
import com.company.common.signature.repository.SignatureDiagramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SignatureService {

    private static final String ATTACHMENT_OWNER_PREFIX = "SIGN_";

    private final SignatureDiagramRepository repository;
    private final AttachmentService attachmentService;

    /**
     * 儲存簽名。
     * 如果已有簽名，舊的整筆軟刪除（content + 附件都保留），新建一筆。
     */
    @Transactional
    public SignatureResponse save(SignatureSaveRequest request, MultipartFile image) throws IOException {
        // 軟刪除舊簽名（整筆保留，標記 deleted=true）
        repository.findActiveByOwner(request.ownerType(), request.ownerId())
                .ifPresent(old -> {
                    // 舊附件也軟刪除
                    if (old.getAttachmentId() != null) {
                        try {
                            attachmentService.softDelete(old.getAttachmentId());
                        } catch (Exception e) {
                            log.warn("軟刪除舊簽名附件失敗: attachmentId={}", old.getAttachmentId(), e);
                        }
                    }
                    old.delete();
                    repository.save(old);
                    log.info("舊簽名已軟刪除: id={}, ownerType={}, ownerId={}",
                            old.getId(), old.getOwnerType(), old.getOwnerId());
                });

        // 新建一筆
        SignatureDiagram diagram = new SignatureDiagram();
        diagram.setOwnerType(request.ownerType());
        diagram.setOwnerId(request.ownerId());
        diagram.setContent(request.json());

        // 上傳新附件
        if (image != null && !image.isEmpty()) {
            String attachOwnerType = ATTACHMENT_OWNER_PREFIX + request.ownerType();
            AttachmentUploadRequest uploadRequest = new AttachmentUploadRequest(
                    attachOwnerType,
                    request.ownerId(),
                    image.getOriginalFilename(),
                    "簽名圖片",
                    image.getInputStream(),
                    image.getSize(),
                    image.getContentType()
            );
            AttachmentUploadResponse uploadResponse = attachmentService.upload(uploadRequest);
            diagram.setAttachmentId(uploadResponse.id());
        }

        diagram = repository.save(diagram);
        log.info("簽名儲存完成: id={}, ownerType={}, ownerId={}",
                diagram.getId(), diagram.getOwnerType(), diagram.getOwnerId());

        return toResponse(diagram);
    }

    @Transactional(readOnly = true)
    public SignatureResponse findByOwner(String ownerType, Long ownerId) {
        return repository.findActiveByOwner(ownerType, ownerId)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * 刪除簽名（簽名 + 附件都軟刪除，資料保留可追溯）。
     */
    @Transactional
    public void delete(String ownerType, Long ownerId) {
        repository.findActiveByOwner(ownerType, ownerId)
                .ifPresent(diagram -> {
                    if (diagram.getAttachmentId() != null) {
                        try {
                            attachmentService.softDelete(diagram.getAttachmentId());
                        } catch (Exception e) {
                            log.warn("軟刪除簽名附件失敗: attachmentId={}", diagram.getAttachmentId(), e);
                        }
                    }
                    diagram.delete();
                    repository.save(diagram);
                    log.info("簽名已軟刪除: ownerType={}, ownerId={}", ownerType, ownerId);
                });
    }

    @Transactional(readOnly = true)
    public List<AttachmentUploadResponse> getAttachments(String ownerType, Long ownerId) {
        String attachOwnerType = ATTACHMENT_OWNER_PREFIX + ownerType;
        return attachmentService.findByOwner(new AttachmentOwnerRef(attachOwnerType, ownerId));
    }

    private SignatureResponse toResponse(SignatureDiagram diagram) {
        return new SignatureResponse(
                diagram.getId(),
                diagram.getOwnerType(),
                diagram.getOwnerId(),
                diagram.getContent(),
                diagram.getAttachmentId(),
                diagram.getCreatedBy(),
                diagram.getCreatedDate(),
                diagram.getLastModifiedBy(),
                diagram.getLastModifiedDate()
        );
    }
}
