package com.company.common.report.repository;

import com.company.common.report.entity.ReportLogBlob;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 報表檔案 BLOB Repository
 */
public interface ReportLogBlobRepository extends JpaRepository<ReportLogBlob, Long> {
}
