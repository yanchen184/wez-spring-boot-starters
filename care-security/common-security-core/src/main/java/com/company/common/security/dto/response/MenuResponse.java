package com.company.common.security.dto.response;

public record MenuResponse(
        Long id,
        String menuName,
        String menuCode,
        String url,
        String type,
        Long parentId,
        Integer sortOrder,
        String icon,
        Boolean enabled,
        String cPermAlias,
        String rPermAlias,
        String uPermAlias,
        String dPermAlias
) {}
