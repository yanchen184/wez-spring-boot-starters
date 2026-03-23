package com.company.common.diagram.dto;

import java.time.LocalDateTime;

public record DiagramResponse(
        Long id,
        String diagramType,
        String ownerType,
        Long ownerId,
        String name,
        String content,
        Long attachmentId,
        boolean deleted,
        String createdBy,
        LocalDateTime createdDate,
        String lastModifiedBy,
        LocalDateTime lastModifiedDate
) {
}
