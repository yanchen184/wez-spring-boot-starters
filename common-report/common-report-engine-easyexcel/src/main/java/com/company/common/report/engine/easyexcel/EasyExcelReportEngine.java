package com.company.common.report.engine.easyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.enums.WriteDirectionEnum;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * EasyExcel 報表引擎實作
 *
 * <p>支援兩種模式：
 * <ul>
 *   <li>資料寫入模式：context.getData() 不為空時，直接將 List 資料寫入 Excel</li>
 *   <li>範本填充模式：context.getTemplatePath() 不為空時，使用範本填充資料</li>
 * </ul>
 */
public class EasyExcelReportEngine implements ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(EasyExcelReportEngine.class);

    private static final Set<OutputFormat> SUPPORTED_FORMATS = Set.of(
            OutputFormat.XLSX, OutputFormat.XLS, OutputFormat.CSV
    );

    @Override
    public ReportEngineType getType() {
        return ReportEngineType.EASYEXCEL;
    }

    @Override
    public boolean supports(OutputFormat format) {
        return SUPPORTED_FORMATS.contains(format);
    }

    @Override
    public ReportResult generate(ReportContext context) {
        log.info("--> EasyExcel generate | template={}, format={}, dataSize={}",
                context.getTemplatePath(), context.getOutputFormat(),
                context.getData() != null ? context.getData().size() : 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelTypeEnum excelType = toExcelType(context.getOutputFormat());

        if (context.getTemplatePath() != null && !context.getTemplatePath().isBlank()) {
            generateWithTemplate(context, out, excelType);
        } else if (context.getData() != null && !context.getData().isEmpty()) {
            generateWithData(context, out, excelType);
        } else {
            throw new IllegalArgumentException(
                    "Either templatePath or data must be provided for EasyExcel engine");
        }

        String fileName = resolveFileName(context);
        String contentType = resolveContentType(context.getOutputFormat());

        log.info("<-- EasyExcel generate | fileName={}, size={} bytes", fileName, out.size());
        return new ReportResult(out.toByteArray(), contentType, fileName);
    }

    /**
     * 使用範本填充模式
     */
    private void generateWithTemplate(ReportContext context,
                                      ByteArrayOutputStream out,
                                      ExcelTypeEnum excelType) {
        InputStream templateStream = getClass().getClassLoader()
                .getResourceAsStream(context.getTemplatePath());
        if (templateStream == null) {
            throw new IllegalArgumentException(
                    "Template not found: " + context.getTemplatePath());
        }

        try (templateStream;
             ExcelWriter writer = EasyExcel.write(out)
                .excelType(excelType)
                .withTemplate(templateStream)
                .build()) {

            WriteSheet sheet = EasyExcel.writerSheet().build();
            FillConfig fillConfig = FillConfig.builder()
                    .direction(WriteDirectionEnum.VERTICAL)
                    .forceNewRow(Boolean.FALSE)
                    .build();

            // 填充參數
            if (context.getParameters() != null && !context.getParameters().isEmpty()) {
                writer.fill(context.getParameters(), sheet);
            }
            // 填充資料列表
            if (context.getData() != null && !context.getData().isEmpty()) {
                writer.fill(context.getData(), fillConfig, sheet);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate report from template", e);
        }
    }

    /**
     * 直接資料寫入模式
     */
    private void generateWithData(ReportContext context,
                                  ByteArrayOutputStream out,
                                  ExcelTypeEnum excelType) {
        if (context.getData() == null || context.getData().isEmpty()) {
            throw new IllegalArgumentException("Data list cannot be empty for data-driven report");
        }
        // 取得資料的 Class（用於 EasyExcel 的 head 自動推導）
        Class<?> dataClass = context.getData().getFirst().getClass();

        EasyExcel.write(out, dataClass)
                .excelType(excelType)
                .sheet("Sheet1")
                .doWrite(context.getData());
    }

    private ExcelTypeEnum toExcelType(OutputFormat format) {
        return switch (format) {
            case XLS -> ExcelTypeEnum.XLS;
            case CSV -> ExcelTypeEnum.CSV;
            default -> ExcelTypeEnum.XLSX;
        };
    }

    private String resolveFileName(ReportContext context) {
        if (context.getFileName() != null && !context.getFileName().isBlank()) {
            return context.getFileName();
        }
        return "report." + context.getOutputFormat().name().toLowerCase();
    }

    private String resolveContentType(OutputFormat format) {
        return switch (format) {
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case XLS -> "application/vnd.ms-excel";
            case CSV -> "text/csv";
            default -> "application/octet-stream";
        };
    }
}
