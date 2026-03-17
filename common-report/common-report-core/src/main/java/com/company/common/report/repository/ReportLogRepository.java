package com.company.common.report.repository;

import com.company.common.report.entity.ReportLog;
import com.company.common.report.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 報表記錄 Repository
 */
public interface ReportLogRepository extends JpaRepository<ReportLog, Long> {

    Optional<ReportLog> findByUuid(String uuid);

    void deleteByCreatedDateBeforeAndStatus(LocalDateTime date, ReportStatus status);
}
