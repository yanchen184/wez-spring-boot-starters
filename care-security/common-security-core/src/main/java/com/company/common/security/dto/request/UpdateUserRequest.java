package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "PUT /api/users/{id}")
public record UpdateUserRequest(
        @Schema(description = "中文姓名")
        @Size(max = 255)
        String cname,
        @Email
        @Size(max = 255)
        String email,
        Boolean enabled,
        @Schema(description = "角色 ID 清單", example = "[1, 3]")
        List<Long> roleIds
) {}
