package com.company.common.security.service;

import com.company.common.security.dto.request.PermRequest;
import com.company.common.security.dto.response.PermResponse;
import com.company.common.security.entity.Menu;
import com.company.common.security.entity.Perm;
import com.company.common.security.repository.MenuRepository;
import com.company.common.security.repository.PermRepository;
import com.company.common.security.repository.RolePermsRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class PermService {

    private final PermRepository permRepository;
    private final MenuRepository menuRepository;
    private final RolePermsRepository rolePermsRepository;

    public PermService(PermRepository permRepository,
                       MenuRepository menuRepository,
                       RolePermsRepository rolePermsRepository) {
        this.permRepository = permRepository;
        this.menuRepository = menuRepository;
        this.rolePermsRepository = rolePermsRepository;
    }

    @Transactional(readOnly = true)
    public List<PermResponse> findAll() {
        return permRepository.findAllWithMenu().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PermResponse create(PermRequest request) {
        Perm perm = new Perm();
        applyFields(perm, request);
        perm.setEnabled(true);
        return toResponse(permRepository.save(perm));
    }

    @Transactional
    public PermResponse update(Long id, PermRequest request) {
        Perm perm = permRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
        applyFields(perm, request);
        return toResponse(permRepository.save(perm));
    }

    @Transactional
    public void delete(Long id) {
        Perm perm = permRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));

        if (rolePermsRepository.existsByPermObjid(id)) {
            throw new IllegalStateException("Cannot delete permission that is assigned to roles. Remove role assignments first.");
        }

        permRepository.delete(perm);
    }

    private void applyFields(Perm perm, PermRequest request) {
        if (request.permCode() != null) perm.setPermCode(request.permCode());
        if (request.menuId() != null) {
            Menu menu = menuRepository.findById(request.menuId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + request.menuId()));
            perm.setMenu(menu);
        } else {
            perm.setMenu(null);
        }
        if (request.canCreate() != null) perm.setCanCreate(request.canCreate());
        if (request.canRead() != null) perm.setCanRead(request.canRead());
        if (request.canUpdate() != null) perm.setCanUpdate(request.canUpdate());
        if (request.canDelete() != null) perm.setCanDelete(request.canDelete());
        if (request.canApprove() != null) perm.setCanApprove(request.canApprove());
    }

    private PermResponse toResponse(Perm perm) {
        Menu menu = perm.getMenu();
        return new PermResponse(
                perm.getObjid(),
                perm.getPermCode(),
                menu != null ? menu.getObjid() : null,
                menu != null ? menu.getMenuName() : null,
                menu != null ? menu.getMenuCode() : null,
                menu != null ? menu.getUrl() : null,
                perm.getCanCreate(),
                perm.getCanRead(),
                perm.getCanUpdate(),
                perm.getCanDelete(),
                perm.getCanApprove(),
                perm.getEnabled()
        );
    }
}
