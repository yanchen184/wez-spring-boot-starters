package com.company.common.security.service;

import com.company.common.security.dto.response.OrganizeTreeResponse;
import com.company.common.security.entity.Organize;
import com.company.common.security.repository.OrganizeRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class OrganizeService {

    private final OrganizeRepository organizeRepository;

    public OrganizeService(OrganizeRepository organizeRepository) {
        this.organizeRepository = organizeRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizeTreeResponse> findAll() {
        return organizeRepository.findAll().stream()
                .filter(org -> Boolean.TRUE.equals(org.getEnabled()))
                .map(org -> new OrganizeTreeResponse(
                        org.getObjid(),
                        org.getOrgName(),
                        org.getOrgCode(),
                        org.getSortOrder(),
                        List.of()))
                .toList();
    }

    /**
     * Builds organization tree with optimized single query to avoid N+1 problem.
     * Loads all enabled organizations at once and assembles tree in memory.
     */
    @Transactional(readOnly = true)
    public List<OrganizeTreeResponse> getOrganizationTree() {
        // Load all enabled organizations in one query
        List<Organize> allOrgs = organizeRepository.findByEnabledTrue();

        // Build map for O(1) lookup
        java.util.Map<Long, Organize> orgMap = allOrgs.stream()
                .collect(java.util.stream.Collectors.toMap(Organize::getObjid, o -> o));

        // Build in-memory parent-child relationships
        for (Organize org : allOrgs) {
            if (org.getParentId() != null) {
                Organize parent = orgMap.get(org.getParentId());
                if (parent != null) {
                    parent.getChildren().add(org);
                }
            }
        }

        // Find roots and convert to tree response
        return allOrgs.stream()
                .filter(o -> o.getParentId() == null)
                .sorted(java.util.Comparator.comparing(Organize::getSortOrder,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(this::toTreeResponseFromMemory)
                .toList();
    }

    /**
     * Converts Organize to TreeResponse using already loaded children from memory.
     * Does not trigger database queries (children already in memory).
     */
    private OrganizeTreeResponse toTreeResponseFromMemory(Organize org) {
        List<OrganizeTreeResponse> children = org.getChildren().stream()
                .sorted(java.util.Comparator.comparing(Organize::getSortOrder,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(this::toTreeResponseFromMemory)
                .toList();

        return new OrganizeTreeResponse(
                org.getObjid(),
                org.getOrgName(),
                org.getOrgCode(),
                org.getSortOrder(),
                children
        );
    }

    /**
     * @deprecated Use toTreeResponseFromMemory() with pre-loaded children to avoid N+1 queries.
     * This method triggers lazy loading of children.
     */
    @Deprecated
    private OrganizeTreeResponse toTreeResponse(Organize org) {
        List<OrganizeTreeResponse> children = org.getChildren().stream()
                .filter(child -> Boolean.TRUE.equals(child.getEnabled()))
                .map(this::toTreeResponse)
                .toList();

        return new OrganizeTreeResponse(
                org.getObjid(),
                org.getOrgName(),
                org.getOrgCode(),
                org.getSortOrder(),
                children
        );
    }
}
