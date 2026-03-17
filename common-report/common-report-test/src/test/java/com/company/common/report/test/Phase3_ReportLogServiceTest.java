package com.company.common.report.test;

import com.company.common.report.entity.ReportLog;
import com.company.common.report.enums.ReportStatus;
import com.company.common.report.service.ReportLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 3: Report Log Service (Audit & Status Management)
 *
 * === TDD Guide for Engineers ===
 *
 * 3.1 Create Log
 *     -> What: Creates a PENDING report log with UUID
 *     -> Why: "As a system, I track every report generation request"
 *
 * 3.2 Status Transitions
 *     -> What: Status follows PENDING -> PROCESSING -> COMPLETED/FAILED
 *     -> Why: "As a system, I enforce valid state machine transitions"
 *
 * 3.3 Save Result
 *     -> What: Stores generated file in ReportLogBlob
 *     -> Why: "As a user, I download previously generated reports"
 *
 * === Implementation Checklist ===
 * [x] createLog() -- generates UUID, sets PENDING status
 * [x] updateStatus() -- validates state transitions
 * [x] saveResult() -- stores file blob
 * [x] findByUuid() / getBlob()
 */
@ReportTest
@DisplayName("Phase 3: Report Log Service")
class Phase3_ReportLogServiceTest {

    @Autowired
    ReportLogService logService;

    // ========================================================================
    // 3.1 Create Log
    //
    // User Story: As a system, I track every report generation request.
    //
    // Acceptance Criteria:
    //   - createLog() returns a non-blank UUID (36 chars)
    //   - The created log has PENDING status
    //   - startTime is set automatically
    //   - Each call generates a different UUID
    // ========================================================================

    @Nested
    @DisplayName("3.1 Create Log")
    class CreateLog {

        @Test
        @DisplayName("creates log with PENDING status and unique UUID")
        void createPendingLog() {
            String uuid = logService.createLog("sales-report", "sales.xlsx");

            assertThat(uuid).isNotBlank();
            assertThat(uuid).hasSize(36);

            Optional<ReportLog> log = logService.findByUuid(uuid);
            assertThat(log).isPresent();
            assertThat(log.get().getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(log.get().getReportName()).isEqualTo("sales-report");
            assertThat(log.get().getStartTime()).isNotNull();
        }

        @Test
        @DisplayName("each call generates different UUID")
        void uniqueUuids() {
            String uuid1 = logService.createLog("report1", "r1.xlsx");
            String uuid2 = logService.createLog("report2", "r2.xlsx");

            assertThat(uuid1).isNotEqualTo(uuid2);
        }
    }

    // ========================================================================
    // 3.2 Status Transitions
    //
    // User Story: As a system, I enforce valid state machine transitions.
    //
    // Valid transitions:
    //   PENDING -> PROCESSING
    //   PENDING -> FAILED (direct failure, e.g. validation error)
    //   PROCESSING -> COMPLETED
    //   PROCESSING -> FAILED
    //   COMPLETED -> any (rejected, terminal state)
    //   FAILED -> any (rejected, terminal state)
    // ========================================================================

    @Nested
    @DisplayName("3.2 Status Transitions")
    class StatusTransitions {

        @Test
        @DisplayName("PENDING -> PROCESSING is allowed")
        void pendingToProcessing() {
            String uuid = logService.createLog("report", "r.xlsx");
            logService.updateStatus(uuid, ReportStatus.PROCESSING, null);

            assertThat(logService.findByUuid(uuid).get().getStatus())
                    .isEqualTo(ReportStatus.PROCESSING);
        }

        @Test
        @DisplayName("PROCESSING -> COMPLETED is allowed")
        void processingToCompleted() {
            String uuid = logService.createLog("report", "r.xlsx");
            logService.updateStatus(uuid, ReportStatus.PROCESSING, null);
            logService.updateStatus(uuid, ReportStatus.COMPLETED, null);

            ReportLog log = logService.findByUuid(uuid).get();
            assertThat(log.getStatus()).isEqualTo(ReportStatus.COMPLETED);
            assertThat(log.getEndTime()).isNotNull();
        }

        @Test
        @DisplayName("PROCESSING -> FAILED is allowed")
        void processingToFailed() {
            String uuid = logService.createLog("report", "r.xlsx");
            logService.updateStatus(uuid, ReportStatus.PROCESSING, null);
            logService.updateStatus(uuid, ReportStatus.FAILED, "Out of memory");

            ReportLog log = logService.findByUuid(uuid).get();
            assertThat(log.getStatus()).isEqualTo(ReportStatus.FAILED);
            assertThat(log.getErrorMessage()).isEqualTo("Out of memory");
            assertThat(log.getEndTime()).isNotNull();
        }

        @Test
        @DisplayName("COMPLETED -> any is rejected (terminal state)")
        void completedIsTerminal() {
            String uuid = logService.createLog("report", "r.xlsx");
            logService.updateStatus(uuid, ReportStatus.PROCESSING, null);
            logService.updateStatus(uuid, ReportStatus.COMPLETED, null);

            assertThatThrownBy(() ->
                    logService.updateStatus(uuid, ReportStatus.PROCESSING, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("FAILED -> any is rejected (terminal state)")
        void failedIsTerminal() {
            String uuid = logService.createLog("report", "r.xlsx");
            logService.updateStatus(uuid, ReportStatus.PROCESSING, null);
            logService.updateStatus(uuid, ReportStatus.FAILED, "error");

            assertThatThrownBy(() ->
                    logService.updateStatus(uuid, ReportStatus.PROCESSING, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("PENDING -> FAILED is allowed (direct failure)")
        void pendingToFailed() {
            String uuid = logService.createLog("report", "r.xlsx");
            logService.updateStatus(uuid, ReportStatus.FAILED, "Validation error");

            assertThat(logService.findByUuid(uuid).get().getStatus())
                    .isEqualTo(ReportStatus.FAILED);
        }
    }

    // ========================================================================
    // 3.3 Save Result
    //
    // User Story: As a user, I download previously generated reports.
    //
    // Acceptance Criteria:
    //   - saveResult() stores file content as blob
    //   - getBlob() retrieves the stored content
    //   - getBlob() returns empty for non-existent uuid
    // ========================================================================

    @Nested
    @DisplayName("3.3 Save Result")
    class SaveResult {

        @Test
        @DisplayName("saves file blob and retrieves it")
        void saveAndGetBlob() {
            String uuid = logService.createLog("report", "r.xlsx");
            byte[] content = "Excel file content".getBytes();

            logService.saveResult(uuid, content,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            Optional<byte[]> blob = logService.getBlob(uuid);
            assertThat(blob).isPresent();
            assertThat(new String(blob.get())).isEqualTo("Excel file content");
        }

        @Test
        @DisplayName("getBlob returns empty for non-existent uuid")
        void getBlobNonExistent() {
            assertThat(logService.getBlob("non-existent")).isEmpty();
        }
    }
}
