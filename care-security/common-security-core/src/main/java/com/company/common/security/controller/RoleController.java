package com.company.common.security.controller;

import com.company.common.security.dto.response.PermissionMatrixResponse;
import com.company.common.security.dto.response.RoleResponse;
import com.company.common.security.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Role", description = "Role management (requires ADMIN role)")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "List all roles", description = "Retrieve all roles with their permissions")
    @GetMapping
    public List<RoleResponse> findAll() {
        return roleService.findAll();
    }

    @Operation(summary = "Get role by ID", description = "Retrieve a single role with its permissions")
    @GetMapping("/{id}")
    public RoleResponse findById(@PathVariable Long id) {
        return roleService.findById(id);
    }

    @Operation(summary = "Get permission matrix", description = "Retrieve CRUD permission matrix for a role")
    @GetMapping("/{id}/permissions")
    public PermissionMatrixResponse getPermissionMatrix(@PathVariable Long id) {
        return roleService.getPermissionMatrix(id);
    }

    @Operation(summary = "Update role permissions", description = "Update which permissions are assigned to a role")
    @PutMapping("/{id}/permissions")
    public void updatePermissionMatrix(
            @PathVariable Long id,
            @RequestBody List<Long> permIds,
            Authentication auth) {
        roleService.updatePermissionMatrix(id, permIds, auth.getName());
    }
}
