package com.company.common.report.dto;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 報表產製請求
 */
public record ReportGenerateRequest(
        @NotBlank String templatePath,
        @NotNull ReportEngineType engineType,
        @NotNull OutputFormat outputFormat,
        String fileName,
        Map<String, Object> parameters
) {
}
