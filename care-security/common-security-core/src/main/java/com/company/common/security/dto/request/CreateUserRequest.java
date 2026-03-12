package com.company.common.security.dto.request;

import com.company.common.security.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "POST /api/users")
public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 255)
        String username,
        @NotBlank(message = "Password is required")
        @ValidPassword
        String password,
        @Schema(description = "中文姓名")
        @Size(max = 255)
        String cname,
        @Email
        @Size(max = 255)
        String email,
        @Schema(description = "角色 ID 清單", example = "[1, 3]")
        List<Long> roleIds
) {}
