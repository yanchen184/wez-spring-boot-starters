package com.company.common.report.controller;

import com.company.common.report.dto.ReportGenerateRequest;
import com.company.common.report.dto.ReportStatusResponse;
import com.company.common.report.entity.ReportLog;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.enums.ReportStatus;
import com.company.common.report.service.ReportAsyncService;
import com.company.common.report.service.ReportLogService;
import com.company.common.report.service.ReportService;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportResult;
import com.company.common.response.dto.ApiResponse;
import com.company.common.response.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 報表產製與下載 Controller
 */
@Tag(name = "Report", description = "Report generation and download")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportLogService logService;
    private final ReportAsyncService asyncService;

    public ReportController(ReportService reportService,
                            ReportLogService logService,
                            ReportAsyncService asyncService) {
        this.reportService = reportService;
        this.logService = logService;
        this.asyncService = asyncService;
    }

    /**
     * 同步產製報表，直接回傳檔案
     */
    @Operation(summary = "同步產製報表")
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody ReportGenerateRequest request) {
        ReportContext context = toContext(request);
        ReportResult result = reportService.generate(context);
        return toFileResponse(result);
    }

    /**
     * 非同步產製報表，回傳 uuid 供後續查詢
     */
    @Operation(summary = "非同步產製報表")
    @PostMapping("/generate-async")
    public ApiResponse<String> generateAsync(@Valid @RequestBody ReportGenerateRequest request) {
        if (asyncService == null) {
            throw BusinessException.badRequest("Async report generation is not enabled");
        }
        ReportContext context = toContext(request);
        String fileName = request.fileName() != null
                ? request.fileName()
                : "report." + request.outputFormat().name().toLowerCase();
        String uuid = logService.createLog(request.templatePath(), fileName);
        asyncService.generateAsync(context, uuid);
        return ApiResponse.ok("Report generation started", uuid);
    }

    /**
     * 查詢非同步報表產製狀態
     */
    @Operation(summary = "查詢報表產製狀態")
    @GetMapping("/status/{uuid}")
    public ApiResponse<ReportStatusResponse> getStatus(@PathVariable String uuid) {
        ReportLog reportLog = logService.findByUuid(uuid)
                .orElseThrow(() -> BusinessException.notFound("Report not found: " + uuid));
        ReportStatusResponse response = new ReportStatusResponse(
                reportLog.getUuid(),
                reportLog.getStatus(),
                reportLog.getFileName(),
                reportLog.getErrorMessage(),
                reportLog.getStartTime(),
                reportLog.getEndTime()
        );
        return ApiResponse.ok(response);
    }

    /**
     * 下載已完成的報表檔案
     */
    @Operation(summary = "下載報表檔案")
    @GetMapping("/download/{uuid}")
    public void download(@PathVariable String uuid,
                         jakarta.servlet.http.HttpServletResponse response) throws Exception {
        ReportLog reportLog = logService.findByUuid(uuid)
                .orElseThrow(() -> BusinessException.notFound("Report not found: " + uuid));

        if (reportLog.getStatus() != ReportStatus.COMPLETED) {
            throw BusinessException.badRequest(
                    "Cannot download report in " + reportLog.getStatus() + " status");
        }

        byte[] content = logService.getBlob(uuid)
                .orElseThrow(() -> BusinessException.notFound("Report file not found: " + uuid));

        String contentType = reportLog.getContentType() != null
                ? reportLog.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        String encodedFileName = java.net.URLEncoder.encode(
                reportLog.getFileName(), StandardCharsets.UTF_8);
        response.setContentType(contentType);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encodedFileName + "\"");
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
        response.getOutputStream().flush();
    }

    /**
     * 列出目前可用的引擎類型
     */
    @Operation(summary = "列出可用引擎")
    @GetMapping("/engines")
    public ApiResponse<Set<ReportEngineType>> getEngines() {
        return ApiResponse.ok(reportService.getAvailableEngines());
    }

    // ===== Private helpers =====

    private ReportContext toContext(ReportGenerateRequest request) {
        ReportContext context = new ReportContext();
        context.setTemplatePath(request.templatePath());
        context.setEngineType(request.engineType());
        context.setOutputFormat(request.outputFormat());
        context.setFileName(request.fileName());
        if (request.parameters() != null) {
            context.setParameters(request.parameters());
        }
        return context;
    }

    private ResponseEntity<byte[]> toFileResponse(ReportResult result) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.getContentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(result.getFileName(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(result.getContent());
    }
}
