package com.company.common.security.service;

import com.company.common.security.dto.response.OrgRoleResponse;
import com.company.common.security.entity.SaUserOrgRole;
import com.company.common.security.repository.SaUserOrgRoleRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrgRoleService {

    private final SaUserOrgRoleRepository saUserOrgRoleRepository;

    public OrgRoleService(SaUserOrgRoleRepository saUserOrgRoleRepository) {
        this.saUserOrgRoleRepository = saUserOrgRoleRepository;
    }

    @Transactional(readOnly = true)
    public List<OrgRoleResponse> findAllGroupedByOrg() {
        List<SaUserOrgRole> allOrgRoles = saUserOrgRoleRepository.findAllWithRelations();

        Map<Long, List<SaUserOrgRole>> grouped = allOrgRoles.stream()
                .collect(Collectors.groupingBy(r -> r.getOrganize().getObjid()));

        return grouped.values().stream()
                .map(roles -> {
                    SaUserOrgRole first = roles.getFirst();
                    List<OrgRoleResponse.OrgRoleAssignment> assignments = roles.stream()
                            .map(r -> new OrgRoleResponse.OrgRoleAssignment(
                                    r.getObjid(),
                                    r.getSaUser().getObjid(),
                                    r.getSaUser().getUsername(),
                                    r.getSaUser().getCname(),
                                    r.getRole().getObjid(),
                                    r.getRole().getAuthority()))
                            .toList();
                    return new OrgRoleResponse(
                            first.getOrganize().getObjid(),
                            first.getOrganize().getOrgName(),
                            first.getOrganize().getOrgCode(),
                            assignments);
                })
                .sorted(Comparator.comparing(OrgRoleResponse::orgId))
                .toList();
    }
}
