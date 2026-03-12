package com.company.common.security.service;

import com.company.common.security.dto.response.PermissionMatrixResponse;
import com.company.common.security.dto.response.RoleResponse;
import com.company.common.security.entity.Perm;
import com.company.common.security.entity.Role;
import com.company.common.security.entity.RolePerms;
import com.company.common.security.repository.PermRepository;
import com.company.common.security.repository.RolePermsRepository;
import com.company.common.security.repository.RoleRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class RoleService {

    private final RoleRepository roleRepository;
    private final PermRepository permRepository;
    private final RolePermsRepository rolePermsRepository;
    private final AuditService auditService;

    public RoleService(RoleRepository roleRepository,
                       PermRepository permRepository,
                       RolePermsRepository rolePermsRepository,
                       AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permRepository = permRepository;
        this.rolePermsRepository = rolePermsRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        return toResponse(role);
    }

    @Transactional(readOnly = true)
    public PermissionMatrixResponse getPermissionMatrix(Long roleId) {
        Role role = roleRepository.findByIdWithPerms(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        List<Perm> perms = permRepository.findByRoleIdWithMenu(roleId);

        List<PermissionMatrixResponse.PermEntry> entries = perms.stream()
                .map(p -> new PermissionMatrixResponse.PermEntry(
                        p.getObjid(),
                        p.getPermCode(),
                        p.getMenu() != null ? p.getMenu().getMenuName() : p.getPermCode(),
                        Boolean.TRUE.equals(p.getCanCreate()),
                        Boolean.TRUE.equals(p.getCanRead()),
                        Boolean.TRUE.equals(p.getCanUpdate()),
                        Boolean.TRUE.equals(p.getCanDelete()),
                        Boolean.TRUE.equals(p.getCanApprove())))
                .toList();

        return new PermissionMatrixResponse(role.getObjid(), role.getAuthority(), entries);
    }

    @Transactional
    public void updatePermissionMatrix(Long roleId, List<Long> permIds, String operator) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        // Clear existing permissions for this role
        rolePermsRepository.deleteByRoleObjid(roleId);

        // Add new role-perm associations
        for (Long permId : permIds) {
            Perm perm = permRepository.findById(permId)
                    .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permId));

            RolePerms rolePerms = new RolePerms();
            rolePerms.setRole(role);
            rolePerms.setPerm(perm);
            rolePermsRepository.save(rolePerms);
        }

        auditService.logEvent("PERMISSION_UPDATE", operator, null,
                null, null, "Permission matrix updated for role: " + role.getAuthority());
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getObjid(),
                role.getAuthority(),
                role.getDescription(),
                role.getEnabled()
        );
    }
}
