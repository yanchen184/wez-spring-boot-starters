package com.company.common.security.controller;

import com.company.common.response.dto.ApiResponse;
import com.company.common.security.dto.response.OrganizeTreeResponse;
import com.company.common.security.service.OrganizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Organize", description = "Organization management")
@RestController
@RequestMapping("/api/orgs")
public class OrganizeController {

    private final OrganizeService organizeService;

    public OrganizeController(OrganizeService organizeService) {
        this.organizeService = organizeService;
    }

    @Operation(summary = "Get all organizations (flat list)", description = "Retrieve all enabled organizations as a flat list for dropdowns")
    @GetMapping
public ResponseEntity<ApiResponse<List<OrganizeTreeResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(organizeService.findAll()));
    }

    @Operation(summary = "Get organization tree", description = "Retrieve the full organization tree structure")
    @GetMapping("/tree")
public ResponseEntity<ApiResponse<List<OrganizeTreeResponse>>> getOrganizationTree() {
        return ResponseEntity.ok(ApiResponse.ok(organizeService.getOrganizationTree()));
    }
}
