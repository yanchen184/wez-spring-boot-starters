package com.company.common.report.dto;

import com.company.common.report.enums.ReportStatus;

import java.time.LocalDateTime;

/**
 * 報表狀態回應
 */
public record ReportStatusResponse(
        String uuid,
        ReportStatus status,
        String fileName,
        String errorMessage,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
