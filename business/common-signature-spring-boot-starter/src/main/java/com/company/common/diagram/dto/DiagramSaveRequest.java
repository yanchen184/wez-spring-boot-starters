package com.company.common.diagram.dto;

public record DiagramSaveRequest(
        String diagramType,
        String ownerType,
        Long ownerId,
        String name,
        String json
) {
}
