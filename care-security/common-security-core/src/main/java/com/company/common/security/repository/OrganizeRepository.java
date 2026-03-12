package com.company.common.security.repository;

import com.company.common.security.entity.Organize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizeRepository extends JpaRepository<Organize, Long> {

    List<Organize> findByParentIdIsNullOrderBySortOrder();

    /**
     * Finds all enabled organizations for efficient tree building without N+1 queries.
     * Load all organizations once and assemble tree in memory.
     */
    List<Organize> findByEnabledTrue();
}
