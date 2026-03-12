package com.company.common.security.dto.response;

import java.util.List;

public record MenuTreeResponse(
        Long id,
        String menuName,
        String menuCode,
        String url,
        String icon,
        Integer sortOrder,
        List<MenuTreeResponse> children
) {}
