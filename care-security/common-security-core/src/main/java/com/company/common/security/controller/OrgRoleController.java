package com.company.common.security.controller;

import com.company.common.security.dto.response.OrgRoleResponse;
import com.company.common.security.service.OrgRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "OrgRole", description = "Organization-Role assignment management")
@RestController
@RequestMapping("/api/org-roles")
public class OrgRoleController {

    private final OrgRoleService orgRoleService;

    public OrgRoleController(OrgRoleService orgRoleService) {
        this.orgRoleService = orgRoleService;
    }

    @Operation(summary = "Get all org-role assignments grouped by organization")
    @GetMapping
    public List<OrgRoleResponse> findAllGroupedByOrg() {
        return orgRoleService.findAllGroupedByOrg();
    }
}
