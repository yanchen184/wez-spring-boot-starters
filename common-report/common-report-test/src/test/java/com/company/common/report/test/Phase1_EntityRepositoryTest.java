package com.company.common.report.test;

import com.company.common.report.entity.ReportItem;
import com.company.common.report.entity.ReportLog;
import com.company.common.report.entity.ReportLogBlob;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.enums.ReportStatus;
import com.company.common.report.repository.ReportItemRepository;
import com.company.common.report.repository.ReportLogBlobRepository;
import com.company.common.report.repository.ReportLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 1: Entity & Repository
 *
 * === TDD Guide for Engineers ===
 *
 * 1.1 ReportItem Entity
 *     -> What: REPORT_ITEM table stores report definitions
 *     -> Why: "As an admin, I define which reports are available"
 *
 * 1.2 ReportLog Entity
 *     -> What: REPORT_LOG table records every report generation
 *     -> Why: "As an auditor, I see who generated what report and when"
 *
 * 1.3 ReportLogBlob Entity
 *     -> What: REPORT_LOG_BLOB stores generated file content
 *     -> Why: "As a user, I can download previously generated reports"
 *
 * === Implementation Checklist ===
 * [x] ReportItem -- name, templatePath, engineType, outputFormat, enabled
 * [x] ReportLog -- uuid, reportName, fileName, contentType, status, startTime, endTime
 * [x] ReportLogBlob -- fileBlob, errorLog (one-to-one with ReportLog)
 * [x] ReportItemRepository -- findByNameAndEnabledTrue, findAllByEnabledTrue
 * [x] ReportLogRepository -- findByUuid
 */
@ReportTest
@DisplayName("Phase 1: Entity & Repository")
class Phase1_EntityRepositoryTest {

    @Autowired
    ReportItemRepository itemRepo;

    @Autowired
    ReportLogRepository logRepo;

    @Autowired
    ReportLogBlobRepository blobRepo;

    // ========================================================================
    // 1.1 ReportItem
    //
    // User Story: As an admin, I define which reports are available.
    //
    // Acceptance Criteria:
    //   - Can save a ReportItem with name, templatePath, engineType, outputFormat
    //   - findByNameAndEnabledTrue returns enabled items only
    //   - findAllByEnabledTrue returns all enabled items
    // ========================================================================

    @Nested
    @DisplayName("1.1 ReportItem")
    class ReportItemTests {

        @Test
        @DisplayName("save and find report item")
        void saveAndFind() {
            ReportItem item = new ReportItem();
            item.setName("monthly-sales");
            item.setTemplatePath("templates/monthly-sales.xlsx");
            item.setEngineType(ReportEngineType.EASYEXCEL);
            item.setOutputFormat("XLSX");
            item.setDescription("Monthly sales report");
            item.setEnabled(true);
            itemRepo.save(item);

            assertThat(item.getId()).isNotNull();

            Optional<ReportItem> found = itemRepo.findByNameAndEnabledTrue("monthly-sales");
            assertThat(found).isPresent();
            assertThat(found.get().getTemplatePath()).isEqualTo("templates/monthly-sales.xlsx");
            assertThat(found.get().getEngineType()).isEqualTo(ReportEngineType.EASYEXCEL);
        }

        @Test
        @DisplayName("findByNameAndEnabledTrue excludes disabled items")
        void findExcludesDisabled() {
            ReportItem item = new ReportItem();
            item.setName("disabled-report");
            item.setTemplatePath("templates/disabled.xlsx");
            item.setEngineType(ReportEngineType.EASYEXCEL);
            item.setEnabled(false);
            itemRepo.save(item);

            assertThat(itemRepo.findByNameAndEnabledTrue("disabled-report")).isEmpty();
        }

        @Test
        @DisplayName("findAllByEnabledTrue returns only enabled items")
        void findAllEnabled() {
            long beforeCount = itemRepo.findAllByEnabledTrue().size();

            ReportItem enabled = new ReportItem();
            enabled.setName("enabled-" + UUID.randomUUID());
            enabled.setEngineType(ReportEngineType.EASYEXCEL);
            enabled.setEnabled(true);
            itemRepo.save(enabled);

            ReportItem disabled = new ReportItem();
            disabled.setName("disabled-" + UUID.randomUUID());
            disabled.setEngineType(ReportEngineType.EASYEXCEL);
            disabled.setEnabled(false);
            itemRepo.save(disabled);

            assertThat(itemRepo.findAllByEnabledTrue().size()).isEqualTo(beforeCount + 1);
        }
    }

    // ========================================================================
    // 1.2 ReportLog
    //
    // User Story: As an auditor, I see who generated what report and when.
    //
    // Acceptance Criteria:
    //   - Can save a ReportLog with uuid, reportName, status, startTime
    //   - findByUuid returns the correct log entry
    //   - findByUuid returns empty for non-existent uuid
    //   - @Version field enables optimistic locking
    // ========================================================================

    @Nested
    @DisplayName("1.2 ReportLog")
    class ReportLogTests {

        @Test
        @DisplayName("save log with uuid and find by uuid")
        void saveAndFindByUuid() {
            String uuid = UUID.randomUUID().toString();

            ReportLog log = new ReportLog();
            log.setUuid(uuid);
            log.setReportName("test-report");
            log.setFileName("test.xlsx");
            log.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            log.setStatus(ReportStatus.PENDING);
            log.setStartTime(LocalDateTime.now());
            logRepo.save(log);

            Optional<ReportLog> found = logRepo.findByUuid(uuid);
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(found.get().getReportName()).isEqualTo("test-report");
        }

        @Test
        @DisplayName("findByUuid returns empty for non-existent uuid")
        void findNonExistent() {
            assertThat(logRepo.findByUuid("non-existent")).isEmpty();
        }

        @Test
        @DisplayName("version field enables optimistic locking")
        void optimisticLocking() {
            ReportLog log = new ReportLog();
            log.setUuid(UUID.randomUUID().toString());
            log.setReportName("version-test");
            log.setStatus(ReportStatus.PENDING);
            log.setStartTime(LocalDateTime.now());
            logRepo.save(log);

            assertThat(log.getVersion()).isNotNull();
        }
    }

    // ========================================================================
    // 1.3 ReportLogBlob
    //
    // User Story: As a user, I can download previously generated reports.
    //
    // Acceptance Criteria:
    //   - Can save a ReportLogBlob linked to a ReportLog
    //   - Blob shares the same ID as the parent ReportLog (@MapsId)
    //   - File content can be retrieved by ID
    // ========================================================================

    @Nested
    @DisplayName("1.3 ReportLogBlob")
    class ReportLogBlobTests {

        @Test
        @DisplayName("save blob linked to report log")
        void saveBlobWithLog() {
            ReportLog log = new ReportLog();
            log.setUuid(UUID.randomUUID().toString());
            log.setReportName("blob-test");
            log.setStatus(ReportStatus.COMPLETED);
            log.setStartTime(LocalDateTime.now());

            ReportLogBlob blob = new ReportLogBlob();
            blob.setReportLog(log);
            blob.setFileBlob("test content".getBytes());
            log.setBlob(blob);

            logRepo.save(log);

            assertThat(log.getId()).isNotNull();
            assertThat(blob.getId()).isEqualTo(log.getId());

            Optional<ReportLogBlob> found = blobRepo.findById(log.getId());
            assertThat(found).isPresent();
            assertThat(new String(found.get().getFileBlob())).isEqualTo("test content");
        }
    }
}
