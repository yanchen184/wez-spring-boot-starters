package com.company.common.report.test;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.service.ReportService;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportResult;
import com.company.common.response.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 2: Report Service (Engine Dispatch)
 *
 * === TDD Guide for Engineers ===
 *
 * 2.1 Engine Discovery
 *     -> What: ReportService discovers all ReportEngine beans
 *     -> Why: "As a developer, I add an engine dependency and it auto-registers"
 *
 * 2.2 Engine Dispatch
 *     -> What: ReportService routes to correct engine by type
 *     -> Why: "As a user, I specify engine type and the right engine handles it"
 *
 * 2.3 Validation
 *     -> What: ReportService validates template path and format support
 *     -> Why: "As a system, I reject invalid requests early"
 *
 * === Implementation Checklist ===
 * [x] ReportService -- collect engines from Spring context
 * [x] ReportService.generate() -- route by engineType
 * [x] ReportService.generate() -- reject unsupported format
 * [x] ReportService.generate() -- reject path traversal
 * [x] ReportService.getAvailableEngines() -- list registered engines
 */
@ReportTest
@DisplayName("Phase 2: Report Service")
class Phase2_ReportServiceTest {

    @Autowired
    ReportService reportService;

    // ========================================================================
    // 2.1 Engine Discovery
    //
    // User Story: As a developer, I add an engine dependency and it auto-registers.
    //
    // Acceptance Criteria:
    //   - EasyExcel engine is auto-discovered when on classpath
    //   - getAvailableEngines() includes EASYEXCEL
    // ========================================================================

    @Nested
    @DisplayName("2.1 Engine Discovery")
    class EngineDiscovery {

        @Test
        @DisplayName("EasyExcel engine is auto-discovered")
        void easyExcelRegistered() {
            assertThat(reportService.getAvailableEngines()).contains(ReportEngineType.EASYEXCEL);
        }
    }

    // ========================================================================
    // 2.2 Engine Dispatch
    //
    // User Story: As a user, I specify engine type and the right engine handles it.
    //
    // Acceptance Criteria:
    //   - generate() with EASYEXCEL type routes to EasyExcelReportEngine
    //   - generate() with non-existent engine type throws BusinessException
    // ========================================================================

    @Nested
    @DisplayName("2.2 Engine Dispatch")
    class EngineDispatch {

        @Test
        @DisplayName("dispatches to correct engine by type")
        void dispatchByType() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.EASYEXCEL)
                    .outputFormat(OutputFormat.XLSX)
                    .fileName("test.xlsx")
                    .data(List.of(List.of("header1", "header2"), List.of("val1", "val2")))
                    .build();

            ReportResult result = reportService.generate(context);
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContentType()).contains("spreadsheetml");
        }

        @Test
        @DisplayName("throws when engine type not found")
        void noEngineFound() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.JASPER)
                    .outputFormat(OutputFormat.PDF)
                    .build();

            assertThatThrownBy(() -> reportService.generate(context))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No report engine found");
        }
    }

    // ========================================================================
    // 2.3 Validation
    //
    // User Story: As a system, I reject invalid requests early.
    //
    // Acceptance Criteria:
    //   - Relative path traversal (../../) is rejected
    //   - Absolute path (/etc/passwd) is rejected
    //   - Unsupported format for engine is rejected
    // ========================================================================

    @Nested
    @DisplayName("2.3 Validation")
    class Validation {

        @Test
        @DisplayName("rejects path traversal in templatePath")
        void rejectPathTraversal() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.EASYEXCEL)
                    .outputFormat(OutputFormat.XLSX)
                    .templatePath("../../etc/passwd")
                    .build();

            assertThatThrownBy(() -> reportService.generate(context))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("path traversal");
        }

        @Test
        @DisplayName("rejects absolute path in templatePath")
        void rejectAbsolutePath() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.EASYEXCEL)
                    .outputFormat(OutputFormat.XLSX)
                    .templatePath("/etc/passwd")
                    .build();

            assertThatThrownBy(() -> reportService.generate(context))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("path traversal");
        }

        @Test
        @DisplayName("rejects unsupported format for engine")
        void rejectUnsupportedFormat() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.EASYEXCEL)
                    .outputFormat(OutputFormat.DOCX)
                    .build();

            assertThatThrownBy(() -> reportService.generate(context))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not support format");
        }
    }
}
