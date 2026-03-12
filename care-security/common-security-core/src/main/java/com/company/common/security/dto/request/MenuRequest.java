package com.company.common.security.dto.request;

public record MenuRequest(
        String menuName,
        String menuCode,
        String url,
        String type,
        Long parentId,
        Integer sortOrder,
        String icon,
        String cPermAlias,
        String rPermAlias,
        String uPermAlias,
        String dPermAlias
) {}
