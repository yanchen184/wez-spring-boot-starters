package com.company.common.security.repository;

import com.company.common.security.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByParentIdIsNullAndEnabledTrueOrderBySortOrder();

    List<Menu> findByParentIdAndEnabledTrueOrderBySortOrder(Long parentId);

    /**
     * Finds all enabled menus for efficient tree building without N+1 queries.
     * Load all menus once and assemble tree in memory.
     */
    List<Menu> findByEnabledTrue();
}
