package com.company.common.security.service;

import com.company.common.security.dto.request.MenuRequest;
import com.company.common.security.dto.response.MenuResponse;
import com.company.common.security.dto.response.MenuTreeResponse;
import com.company.common.security.entity.Menu;
import com.company.common.security.entity.Perm;
import com.company.common.security.repository.MenuRepository;
import com.company.common.security.repository.PermRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class MenuService {

    private final MenuRepository menuRepository;
    private final PermRepository permRepository;

    public MenuService(MenuRepository menuRepository, PermRepository permRepository) {
        this.menuRepository = menuRepository;
        this.permRepository = permRepository;
    }

    /**
     * Builds menu tree with optimized single query to avoid N+1 problem.
     * Loads all enabled menus at once and assembles tree in memory.
     */
    @Transactional(readOnly = true)
    public List<MenuTreeResponse> getMenuTree() {
        // Load all enabled menus in one query
        List<Menu> allMenus = menuRepository.findByEnabledTrue();

        // Build map for O(1) lookup
        java.util.Map<Long, Menu> menuMap = allMenus.stream()
                .collect(java.util.stream.Collectors.toMap(Menu::getObjid, m -> m));

        // Build in-memory parent-child relationships
        for (Menu menu : allMenus) {
            if (menu.getParentId() != null) {
                Menu parent = menuMap.get(menu.getParentId());
                if (parent != null) {
                    parent.getChildren().add(menu);
                }
            }
        }

        // Find roots and convert to tree response
        return allMenus.stream()
                .filter(m -> m.getParentId() == null)
                .sorted(java.util.Comparator.comparing(Menu::getSortOrder,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(this::toTreeResponseFromMemory)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> findAll() {
        return menuRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MenuResponse create(MenuRequest request) {
        Menu menu = new Menu();
        applyFields(menu, request);
        menu.setEnabled(true);
        return toResponse(menuRepository.save(menu));
    }

    @Transactional
    public MenuResponse update(Long id, MenuRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + id));
        applyFields(menu, request);
        return toResponse(menuRepository.save(menu));
    }

    @Transactional
    public void delete(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + id));

        List<Menu> children = menuRepository.findByParentIdAndEnabledTrueOrderBySortOrder(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete menu with child items. Remove children first.");
        }

        List<Perm> perms = permRepository.findAll().stream()
                .filter(p -> p.getMenu() != null && p.getMenu().getObjid().equals(id))
                .toList();
        if (!perms.isEmpty()) {
            throw new IllegalStateException("Cannot delete menu with associated permissions. Remove permissions first.");
        }

        menuRepository.delete(menu);
    }

    private void applyFields(Menu menu, MenuRequest request) {
        if (request.menuName() != null) menu.setMenuName(request.menuName());
        if (request.menuCode() != null) menu.setMenuCode(request.menuCode());
        if (request.url() != null) menu.setUrl(request.url());
        if (request.type() != null) menu.setType(request.type());
        menu.setParentId(request.parentId());
        if (request.sortOrder() != null) menu.setSortOrder(request.sortOrder());
        if (request.icon() != null) menu.setIcon(request.icon());
        menu.setCPermAlias(request.cPermAlias());
        menu.setRPermAlias(request.rPermAlias());
        menu.setUPermAlias(request.uPermAlias());
        menu.setDPermAlias(request.dPermAlias());
    }

    private MenuResponse toResponse(Menu menu) {
        return new MenuResponse(
                menu.getObjid(),
                menu.getMenuName(),
                menu.getMenuCode(),
                menu.getUrl(),
                menu.getType(),
                menu.getParentId(),
                menu.getSortOrder(),
                menu.getIcon(),
                menu.getEnabled(),
                menu.getCPermAlias(),
                menu.getRPermAlias(),
                menu.getUPermAlias(),
                menu.getDPermAlias()
        );
    }

    /**
     * Converts Menu to TreeResponse using already loaded children from memory.
     * Does not trigger database queries (children already in memory).
     */
    private MenuTreeResponse toTreeResponseFromMemory(Menu menu) {
        List<MenuTreeResponse> children = menu.getChildren().stream()
                .sorted(java.util.Comparator.comparing(Menu::getSortOrder,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(this::toTreeResponseFromMemory)
                .toList();

        return new MenuTreeResponse(
                menu.getObjid(),
                menu.getMenuName(),
                menu.getMenuCode(),
                menu.getUrl(),
                menu.getIcon(),
                menu.getSortOrder(),
                children
        );
    }

    /**
     * @deprecated Use toTreeResponseFromMemory() with pre-loaded children to avoid N+1 queries.
     * This method triggers lazy loading of children.
     */
    @Deprecated
    private MenuTreeResponse toTreeResponse(Menu menu) {
        List<MenuTreeResponse> children = menu.getChildren().stream()
                .filter(child -> Boolean.TRUE.equals(child.getEnabled()))
                .map(this::toTreeResponse)
                .toList();

        return new MenuTreeResponse(
                menu.getObjid(),
                menu.getMenuName(),
                menu.getMenuCode(),
                menu.getUrl(),
                menu.getIcon(),
                menu.getSortOrder(),
                children
        );
    }
}
