package com.company.common.report.service;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import com.company.common.response.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 報表產製服務
 *
 * <p>根據 {@link ReportContext#getEngineType()} 派發到對應的 {@link ReportEngine} 實作。
 */
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final Map<ReportEngineType, ReportEngine> engineMap;
    private final ReportThrottleService throttleService;

    public ReportService(List<ReportEngine> engines, ReportThrottleService throttleService) {
        this.engineMap = engines.stream()
                .collect(Collectors.toMap(
                        ReportEngine::getType,
                        Function.identity(),
                        (existing, duplicate) -> {
                            throw new IllegalStateException(
                                    "Duplicate ReportEngine registered for type: " + existing.getType());
                        }
                ));
        this.throttleService = throttleService;
    }

    /**
     * 同步產製報表
     *
     * @param context 產製上下文
     * @return 產製結果
     * @throws BusinessException 找不到引擎或不支援的格式
     */
    public ReportResult generate(ReportContext context) {
        log.info("--> report dispatch | engine={}, format={}", context.getEngineType(), context.getOutputFormat());
        validateTemplatePath(context.getTemplatePath());
        ReportEngine engine = resolveEngine(context.getEngineType(), context.getOutputFormat());

        // 限流
        String reportName = context.getReportName() != null ? context.getReportName() : context.getFileName();
        boolean throttled = throttleService != null && reportName != null;
        if (throttled) {
            throttleService.acquire(reportName);
        }
        try {
            return engine.generate(context);
        } finally {
            if (throttled) {
                throttleService.release(reportName);
            }
        }
    }

    /**
     * 合併多個 context 產製一份報表
     *
     * <p>所有 context 必須使用相同的引擎類型和輸出格式。
     * 引擎決定如何合併（例如 EasyExcel：每個 context 一個 Sheet）。
     *
     * @param contexts 多個產製上下文
     * @return 合併後的產製結果
     */
    public ReportResult generate(ReportContext... contexts) {
        if (contexts == null || contexts.length == 0) {
            throw BusinessException.badRequest("At least one ReportContext is required");
        }
        if (contexts.length == 1) {
            return generate(contexts[0]);
        }

        // 驗證所有 context 的 engineType 和 outputFormat 一致
        ReportEngineType engineType = contexts[0].getEngineType();
        OutputFormat outputFormat = contexts[0].getOutputFormat();
        for (int i = 1; i < contexts.length; i++) {
            if (contexts[i].getEngineType() != engineType) {
                throw BusinessException.badRequest(
                        "All contexts must have the same engineType, got "
                                + engineType + " and " + contexts[i].getEngineType());
            }
            if (contexts[i].getOutputFormat() != outputFormat) {
                throw BusinessException.badRequest(
                        "All contexts must have the same outputFormat, got "
                                + outputFormat + " and " + contexts[i].getOutputFormat());
            }
        }

        for (ReportContext ctx : contexts) {
            validateTemplatePath(ctx.getTemplatePath());
        }

        log.info("--> report merged dispatch | engine={}, format={}, count={}",
                engineType, outputFormat, contexts.length);

        ReportEngine engine = resolveEngine(engineType, outputFormat);
        return engine.generateMerged(Arrays.asList(contexts));
    }

    private ReportEngine resolveEngine(ReportEngineType engineType, OutputFormat outputFormat) {
        ReportEngine engine = engineMap.get(engineType);
        if (engine == null) {
            throw BusinessException.badRequest(
                    "No report engine found for type: " + engineType);
        }
        if (!engine.supports(outputFormat)) {
            throw BusinessException.badRequest(
                    "Engine " + engineType + " does not support format: " + outputFormat);
        }
        return engine;
    }

    private void validateTemplatePath(String templatePath) {
        if (templatePath == null) {
            return;
        }
        String normalized = templatePath.replace('\\', '/');
        if (normalized.contains("..") || normalized.startsWith("/")
                || normalized.contains("://") || normalized.contains("%")) {
            throw BusinessException.badRequest("Invalid template path: path traversal not allowed");
        }
    }

    /**
     * 取得目前可用的引擎類型
     */
    public Set<ReportEngineType> getAvailableEngines() {
        return engineMap.keySet();
    }
}
