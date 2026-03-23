package com.company.common.report.engine.jasper;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.poi.export.JRXlsExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JasperReports 報表引擎實作
 *
 * <p>支援 .jrxml（自動編譯）和 .jasper（預編譯）範本。
 * <ul>
 *   <li>PDF：透過 JasperExportManager 直接匯出</li>
 *   <li>XLSX：透過 JRXlsxExporter 匯出</li>
 * </ul>
 */
public class JasperReportEngine implements ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(JasperReportEngine.class);

    private static final Set<OutputFormat> SUPPORTED_FORMATS = Set.of(
            OutputFormat.PDF, OutputFormat.XLSX
    );

    public JasperReportEngine() {
        // 關閉字體嚴格檢查，讓 JR 自動 fallback 到系統可用字體
        DefaultJasperReportsContext.getInstance()
                .setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
    }

    @Override
    public ReportEngineType getType() {
        return ReportEngineType.JASPER;
    }

    @Override
    public boolean supports(OutputFormat format) {
        return SUPPORTED_FORMATS.contains(format);
    }

    @Override
    public ReportResult generate(ReportContext context) {
        log.info("--> JasperReports generate | format={}, template={}, dataSize={}",
                context.getOutputFormat(), context.getTemplatePath(),
                context.getData() != null ? context.getData().size() : 0);

        if (context.getTemplatePath() == null || context.getTemplatePath().isBlank()) {
            throw new IllegalArgumentException(
                    "templatePath is required for JasperReports engine");
        }

        try {
            // 1. 載入範本
            JasperReport jasperReport = loadReport(context.getTemplatePath());

            // 2. 建立資料來源
            JRDataSource dataSource = context.getData() != null && !context.getData().isEmpty()
                    ? new JRBeanCollectionDataSource(context.getData())
                    : new JREmptyDataSource();

            // 3. 準備參數
            Map<String, Object> params = context.getParameters() != null
                    ? new HashMap<>(context.getParameters())
                    : new HashMap<>();

            // 4. 填充
            JasperPrint print = JasperFillManager.fillReport(jasperReport, params, dataSource);

            // 5. 匯出
            byte[] content = export(print, context.getOutputFormat());

            if (content.length > MAX_FILE_SIZE) {
                throw new IllegalStateException("Report file exceeds maximum allowed size: 50MB");
            }

            String fileName = resolveFileName(context);
            String contentType = resolveContentType(context.getOutputFormat());

            log.info("<-- JasperReports generate | fileName={}, size={} bytes",
                    fileName, content.length);
            return new ReportResult(content, contentType, fileName);

        } catch (JRException e) {
            throw new IllegalStateException("Failed to generate JasperReport", e);
        }
    }

    @Override
    public ReportResult generateMerged(List<ReportContext> contexts) {
        if (contexts.size() == 1) {
            return generate(contexts.getFirst());
        }

        log.info("--> JasperReports generateMerged | count={}", contexts.size());

        try {
            // 每個 context 產出一個 JasperPrint，合併成一份
            List<JasperPrint> prints = new java.util.ArrayList<>();
            for (ReportContext ctx : contexts) {
                JasperReport report = loadReport(ctx.getTemplatePath());
                JRDataSource ds = ctx.getData() != null && !ctx.getData().isEmpty()
                        ? new JRBeanCollectionDataSource(ctx.getData())
                        : new JREmptyDataSource();
                Map<String, Object> params = ctx.getParameters() != null
                        ? new HashMap<>(ctx.getParameters())
                        : new HashMap<>();
                prints.add(JasperFillManager.fillReport(report, params, ds));
            }

            // 合併頁面到第一個 print
            JasperPrint merged = prints.getFirst();
            for (int i = 1; i < prints.size(); i++) {
                for (var page : prints.get(i).getPages()) {
                    merged.addPage(page);
                }
            }

            byte[] content = export(merged, contexts.getFirst().getOutputFormat());

            String fileName = resolveFileName(contexts.getFirst());
            String contentType = resolveContentType(contexts.getFirst().getOutputFormat());

            log.info("<-- JasperReports generateMerged | fileName={}, size={} bytes",
                    fileName, content.length);
            return new ReportResult(content, contentType, fileName);

        } catch (JRException e) {
            throw new IllegalStateException("Failed to generate merged JasperReport", e);
        }
    }

    private JasperReport loadReport(String templatePath) throws JRException {
        try (InputStream is = loadTemplate(templatePath)) {
            if (templatePath.endsWith(".jasper")) {
                return (JasperReport) JRLoader.loadObject(is);
            } else {
                // .jrxml — 需要先編譯
                return JasperCompileManager.compileReport(is);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load template: " + templatePath, e);
        }
    }

    private InputStream loadTemplate(String templatePath) {
        // 先嘗試 classpath
        InputStream stream = getClass().getClassLoader().getResourceAsStream(templatePath);
        if (stream != null) {
            return stream;
        }
        // 嘗試 filesystem
        try {
            return new FileInputStream(templatePath);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Template not found: " + templatePath, e);
        }
    }

    private byte[] export(JasperPrint print, OutputFormat format) throws JRException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        switch (format) {
            case PDF -> JasperExportManager.exportReportToPdfStream(print, out);
            case XLSX -> exportToXlsx(print, out);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        }
        return out.toByteArray();
    }

    private void exportToXlsx(JasperPrint print, ByteArrayOutputStream out) throws JRException {
        JRXlsExporter exporter = new JRXlsExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));

        SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
        config.setOnePagePerSheet(false);
        config.setDetectCellType(true);
        config.setRemoveEmptySpaceBetweenRows(true);
        exporter.setConfiguration(config);

        exporter.exportReport();
    }

    private String resolveFileName(ReportContext context) {
        if (context.getFileName() != null && !context.getFileName().isBlank()) {
            return context.getFileName();
        }
        return "report" + getExtension(context.getOutputFormat());
    }

    private String getExtension(OutputFormat format) {
        return switch (format) {
            case PDF -> ".pdf";
            case XLSX -> ".xlsx";
            default -> ".pdf";
        };
    }

    private String resolveContentType(OutputFormat format) {
        return switch (format) {
            case PDF -> "application/pdf";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }
}
