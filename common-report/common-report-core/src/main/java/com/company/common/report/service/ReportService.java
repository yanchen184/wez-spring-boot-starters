package com.company.common.report.service;

import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import com.company.common.response.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public ReportService(List<ReportEngine> engines) {
        this.engineMap = engines.stream()
                .collect(Collectors.toMap(ReportEngine::getType, Function.identity()));
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
        ReportEngine engine = engineMap.get(context.getEngineType());
        if (engine == null) {
            throw BusinessException.badRequest(
                    "No report engine found for type: " + context.getEngineType());
        }
        if (!engine.supports(context.getOutputFormat())) {
            throw BusinessException.badRequest(
                    "Engine " + context.getEngineType()
                            + " does not support format: " + context.getOutputFormat());
        }
        return engine.generate(context);
    }

    private void validateTemplatePath(String templatePath) {
        if (templatePath != null && (templatePath.contains("..") || templatePath.startsWith("/"))) {
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
