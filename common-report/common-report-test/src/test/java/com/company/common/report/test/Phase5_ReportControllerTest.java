package com.company.common.report.test;

import com.company.common.report.service.ReportLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 5: Report Controller (HTTP API Contract)
 *
 * === TDD Guide for Engineers ===
 *
 * 5.1 Synchronous Generation
 *     -> What: POST /api/reports/generate returns file directly
 *     -> Why: "As a frontend, I trigger report generation and download immediately"
 *
 * 5.2 Available Engines
 *     -> What: GET /api/reports/engines lists registered engines
 *     -> Why: "As a frontend, I show users which report types are available"
 *
 * 5.3 Download
 *     -> What: GET /api/reports/download/{uuid} returns generated file
 *     -> Why: "As a user, I download a previously generated report"
 *
 * === Implementation Checklist ===
 * [x] POST /api/reports/generate -- returns file bytes + Content-Disposition
 * [x] GET /api/reports/engines -- returns available engine types
 * [x] GET /api/reports/download/{uuid} -- returns 404 for non-existent uuid
 * [x] GET /api/reports/download/{uuid} -- returns 400 for non-COMPLETED report
 */
@ReportTest
@AutoConfigureMockMvc
@DisplayName("Phase 5: Report Controller")
class Phase5_ReportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ReportLogService logService;

    // ========================================================================
    // 5.1 Synchronous Generation
    //
    // User Story: As a frontend, I trigger report generation and download.
    //
    // Acceptance Criteria:
    //   - POST /api/reports/generate with missing required fields returns 400
    //   - POST /api/reports/generate with valid request returns file bytes
    // ========================================================================

    @Nested
    @DisplayName("5.1 Synchronous Generation")
    class SyncGeneration {

        @Test
        @DisplayName("POST /api/reports/generate without templatePath returns 400")
        void generateWithoutTemplatePath() throws Exception {
            String body = """
                    {
                        "templatePath": null,
                        "engineType": "EASYEXCEL",
                        "outputFormat": "XLSX",
                        "fileName": "test.xlsx",
                        "parameters": {}
                    }
                    """;

            mockMvc.perform(post("/api/reports/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ========================================================================
    // 5.2 Available Engines
    //
    // User Story: As a frontend, I show users which report types are available.
    //
    // Acceptance Criteria:
    //   - GET /api/reports/engines returns 200
    //   - Response contains success=true and data array
    // ========================================================================

    @Nested
    @DisplayName("5.2 Available Engines")
    class AvailableEngines {

        @Test
        @DisplayName("GET /api/reports/engines returns available engines")
        void listEngines() throws Exception {
            mockMvc.perform(get("/api/reports/engines"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ========================================================================
    // 5.3 Download
    //
    // User Story: As a user, I download a previously generated report.
    //
    // Acceptance Criteria:
    //   - Non-existent uuid returns 404
    //   - PENDING report returns 400 (not ready for download)
    // ========================================================================

    @Nested
    @DisplayName("5.3 Download")
    class Download {

        @Test
        @DisplayName("download non-existent uuid returns 404")
        void downloadNonExistent() throws Exception {
            mockMvc.perform(get("/api/reports/download/non-existent-uuid"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("download PENDING report returns 400")
        void downloadPending() throws Exception {
            String uuid = logService.createLog("report", "r.xlsx");

            mockMvc.perform(get("/api/reports/download/" + uuid))
                    .andExpect(status().isBadRequest());
        }
    }
}
