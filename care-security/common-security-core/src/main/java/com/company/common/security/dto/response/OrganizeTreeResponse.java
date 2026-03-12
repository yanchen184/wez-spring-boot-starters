package com.company.common.security.dto.response;

import java.util.List;

public record OrganizeTreeResponse(
        Long id,
        String orgName,
        String orgCode,
        Integer sortOrder,
        List<OrganizeTreeResponse> children
) {}
