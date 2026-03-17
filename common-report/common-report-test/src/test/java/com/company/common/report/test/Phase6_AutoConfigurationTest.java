package com.company.common.report.test;

import com.company.common.report.controller.ReportController;
import com.company.common.report.engine.easyexcel.EasyExcelReportEngine;
import com.company.common.report.service.ReportAsyncService;
import com.company.common.report.service.ReportLogService;
import com.company.common.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 6: Auto-Configuration
 *
 * === TDD Guide for Engineers ===
 *
 * 6.1 Bean Loading
 *     -> What: All report beans are auto-configured
 *     -> Why: "As a developer, I add the dependency and everything works"
 *
 * === Implementation Checklist ===
 * [x] ReportService bean exists
 * [x] ReportLogService bean exists
 * [x] ReportAsyncService bean exists (when async enabled)
 * [x] ReportController bean exists
 * [x] EasyExcelReportEngine bean exists
 */
@ReportTest
@DisplayName("Phase 6: Auto-Configuration")
class Phase6_AutoConfigurationTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("ReportService is auto-configured")
    void reportServiceExists() {
        assertThat(context.getBean(ReportService.class)).isNotNull();
    }

    @Test
    @DisplayName("ReportLogService is auto-configured")
    void reportLogServiceExists() {
        assertThat(context.getBean(ReportLogService.class)).isNotNull();
    }

    @Test
    @DisplayName("ReportAsyncService is auto-configured")
    void reportAsyncServiceExists() {
        assertThat(context.getBean(ReportAsyncService.class)).isNotNull();
    }

    @Test
    @DisplayName("ReportController is auto-configured")
    void reportControllerExists() {
        assertThat(context.getBean(ReportController.class)).isNotNull();
    }

    @Test
    @DisplayName("EasyExcelReportEngine is auto-configured")
    void easyExcelEngineExists() {
        assertThat(context.getBean(EasyExcelReportEngine.class)).isNotNull();
    }
}
