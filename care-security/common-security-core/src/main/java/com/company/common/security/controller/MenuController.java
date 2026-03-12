package com.company.common.security.controller;

import com.company.common.security.dto.request.MenuRequest;
import com.company.common.response.dto.ApiResponse;
import com.company.common.security.dto.response.MenuResponse;
import com.company.common.security.dto.response.MenuTreeResponse;
import com.company.common.security.service.MenuService;
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

@Tag(name = "Menu", description = "Menu management")
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "Get menu tree", description = "Retrieve the full menu tree structure")
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<MenuTreeResponse>>> getMenuTree() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getMenuTree()));
    }

    @Operation(summary = "List all menus", description = "Retrieve all menus as a flat list")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.findAll()));
    }

    @Operation(summary = "Create menu", description = "Create a new menu item")
    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(@RequestBody MenuRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Menu created", menuService.create(request)));
    }

    @Operation(summary = "Update menu", description = "Update an existing menu item")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuResponse>> update(@PathVariable Long id, @RequestBody MenuRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Menu updated", menuService.update(id, request)));
    }

    @Operation(summary = "Delete menu", description = "Delete a menu item")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Menu deleted", null));
    }
}
