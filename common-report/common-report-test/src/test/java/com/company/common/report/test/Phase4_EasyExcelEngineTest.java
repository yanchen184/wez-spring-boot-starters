package com.company.common.report.test;

import com.company.common.report.engine.easyexcel.EasyExcelReportEngine;
import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 4: EasyExcel Engine
 *
 * === TDD Guide for Engineers ===
 *
 * 4.1 Format Support
 *     -> What: EasyExcel supports XLSX, XLS, CSV
 *     -> Why: "As a developer, I know which formats this engine handles"
 *
 * 4.2 Data-Driven Generation
 *     -> What: Generate Excel from List data
 *     -> Why: "As a user, I export query results to Excel"
 *
 * 4.3 Error Handling
 *     -> What: Engine throws clear errors for invalid input
 *     -> Why: "As a developer, I get actionable error messages"
 *
 * === Implementation Checklist ===
 * [x] EasyExcelReportEngine.getType() == EASYEXCEL
 * [x] EasyExcelReportEngine.supports(XLSX/XLS/CSV) == true
 * [x] EasyExcelReportEngine.supports(PDF/DOCX) == false
 * [x] generate() with data list produces valid Excel
 * [x] generate() with empty data throws exception
 */
@ReportTest
@DisplayName("Phase 4: EasyExcel Engine")
class Phase4_EasyExcelEngineTest {

    @Autowired(required = false)
    EasyExcelReportEngine easyExcelEngine;

    // ========================================================================
    // 4.1 Format Support
    //
    // User Story: As a developer, I know which formats this engine handles.
    //
    // Acceptance Criteria:
    //   - Engine type is EASYEXCEL
    //   - Supports XLSX, XLS, CSV
    //   - Does not support PDF, DOCX, ODT
    // ========================================================================

    @Nested
    @DisplayName("4.1 Format Support")
    class FormatSupport {

        @Test
        @DisplayName("engine type is EASYEXCEL")
        void engineType() {
            assertThat(easyExcelEngine).isNotNull();
            assertThat(easyExcelEngine.getType()).isEqualTo(ReportEngineType.EASYEXCEL);
        }

        @Test
        @DisplayName("supports XLSX, XLS, CSV")
        void supportedFormats() {
            assertThat(easyExcelEngine.supports(OutputFormat.XLSX)).isTrue();
            assertThat(easyExcelEngine.supports(OutputFormat.XLS)).isTrue();
            assertThat(easyExcelEngine.supports(OutputFormat.CSV)).isTrue();
        }

        @Test
        @DisplayName("does not support PDF, DOCX, ODT")
        void unsupportedFormats() {
            assertThat(easyExcelEngine.supports(OutputFormat.PDF)).isFalse();
            assertThat(easyExcelEngine.supports(OutputFormat.DOCX)).isFalse();
            assertThat(easyExcelEngine.supports(OutputFormat.ODT)).isFalse();
        }
    }

    // ========================================================================
    // 4.2 Data-Driven Generation
    //
    // User Story: As a user, I export query results to Excel.
    //
    // Acceptance Criteria:
    //   - XLSX output has valid ZIP magic bytes (PK)
    //   - CSV output produces non-empty content with text/csv type
    //   - fileName is correctly set in the result
    // ========================================================================

    @Nested
    @DisplayName("4.2 Data-Driven Generation")
    class DataDriven {

        @Test
        @DisplayName("generates XLSX from data list")
        void generateXlsx() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.EASYEXCEL)
                    .outputFormat(OutputFormat.XLSX)
                    .fileName("test.xlsx")
                    .data(List.of(
                            List.of("Name", "Age"),
                            List.of("Alice", 30),
                            List.of("Bob", 25)
                    ))
                    .build();

            ReportResult result = easyExcelEngine.generate(context);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getFileName()).isEqualTo("test.xlsx");
            assertThat(result.getContentType()).contains("spreadsheetml");

            // XLSX is ZIP format: magic bytes PK (0x50 0x4B)
            assertThat(result.getContent()[0]).isEqualTo((byte) 0x50); // P
            assertThat(result.getContent()[1]).isEqualTo((byte) 0x4B); // K
        }

        @Test
        @DisplayName("generates CSV from data list")
        void generateCsv() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.EASYEXCEL)
                    .outputFormat(OutputFormat.CSV)
                    .fileName("test.csv")
                    .data(List.of(
                            List.of("Name", "Age"),
                            List.of("Alice", 30)
                    ))
                    .build();

            ReportResult result = easyExcelEngine.generate(context);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContentType()).isEqualTo("text/csv");
        }
    }

    // ========================================================================
    // 4.3 Error Handling
    //
    // User Story: As a developer, I get actionable error messages.
    //
    // Acceptance Criteria:
    //   - Null data throws IllegalArgumentException
    //   - Empty data throws IllegalArgumentException
    // ========================================================================

    @Nested
    @DisplayName("4.3 Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws when data list is null")
        void nullData() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.EASYEXCEL)
                    .outputFormat(OutputFormat.XLSX)
                    .data(null)
                    .build();

            assertThatThrownBy(() -> easyExcelEngine.generate(context))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when data list is empty")
        void emptyData() {
            ReportContext context = ReportContext.builder()
                    .engineType(ReportEngineType.EASYEXCEL)
                    .outputFormat(OutputFormat.XLSX)
                    .data(List.of())
                    .build();

            assertThatThrownBy(() -> easyExcelEngine.generate(context))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
