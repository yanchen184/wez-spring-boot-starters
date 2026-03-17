package com.company.common.report.repository;

import com.company.common.report.entity.ReportItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 報表項目 Repository
 */
public interface ReportItemRepository extends JpaRepository<ReportItem, Long> {

    Optional<ReportItem> findByNameAndEnabledTrue(String name);

    List<ReportItem> findAllByEnabledTrue();
}
