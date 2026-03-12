package com.company.common.security.controller;

import com.company.common.security.dto.request.PermRequest;
import com.company.common.response.dto.ApiResponse;
import com.company.common.security.dto.response.PermResponse;
import com.company.common.security.service.PermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Permission", description = "Permission management")
@RestController
@RequestMapping("/api/perms")
public class PermController {

    private final PermService permService;

    public PermController(PermService permService) {
        this.permService = permService;
    }

    @Operation(summary = "List all permissions", description = "Retrieve all permissions")
    @GetMapping
public ResponseEntity<ApiResponse<List<PermResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(permService.findAll()));
    }

    @Operation(summary = "Create permission", description = "Create a new permission")
    @PostMapping
public ResponseEntity<ApiResponse<PermResponse>> create(@RequestBody PermRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Permission created", permService.create(request)));
    }

    @Operation(summary = "Update permission", description = "Update an existing permission")
    @PutMapping("/{id}")
public ResponseEntity<ApiResponse<PermResponse>> update(@PathVariable Long id, @RequestBody PermRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Permission updated", permService.update(id, request)));
    }

    @Operation(summary = "Delete permission", description = "Delete a permission")
    @DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        permService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Permission deleted", null));
    }
}
