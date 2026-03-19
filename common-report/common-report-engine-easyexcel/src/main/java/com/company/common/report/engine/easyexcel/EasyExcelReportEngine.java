package com.company.common.report.engine.easyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.enums.WriteDirectionEnum;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.PivotConfig;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import com.company.common.report.spi.SheetData;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * EasyExcel 報表引擎實作
 *
 * <p>支援四種模式：
 * <ul>
 *   <li>多 Sheet 模式：context.getSheets() 不為空時，每個 SheetData 對應一個工作表</li>
 *   <li>範本填充模式：context.getTemplatePath() 不為空時，使用範本填充資料</li>
 *   <li>資料寫入模式：context.getData() 不為空時，直接將 List 資料寫入 Excel</li>
 *   <li>樞紐分析表：context.getPivots() 不為空時，後處理加入 Pivot Table Sheet</li>
 * </ul>
 */
public class EasyExcelReportEngine implements ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(EasyExcelReportEngine.class);

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

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
        log.info("--> EasyExcel generate | template={}, format={}, dataSize={}, sheets={}, pivots={}",
                context.getTemplatePath(), context.getOutputFormat(),
                context.getData() != null ? context.getData().size() : 0,
                context.getSheets() != null ? context.getSheets().size() : 0,
                context.getPivots() != null ? context.getPivots().size() : 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelTypeEnum excelType = toExcelType(context.getOutputFormat());

        if (context.getSheets() != null && !context.getSheets().isEmpty()) {
            generateMultiSheet(context.getSheets(), out, excelType);
        } else if (context.getTemplatePath() != null && !context.getTemplatePath().isBlank()) {
            generateWithTemplate(context, out, excelType);
        } else if (context.getData() != null && !context.getData().isEmpty()) {
            generateWithData(context, out, excelType);
        } else {
            throw new IllegalArgumentException(
                    "Either sheets, templatePath, or data must be provided for EasyExcel engine");
        }

        // Pivot Table 後處理
        if (context.getPivots() != null && !context.getPivots().isEmpty()
                && context.getOutputFormat() == OutputFormat.XLSX) {
            byte[] withPivot = addPivotTables(out.toByteArray(), context.getPivots());
            out.reset();
            try {
                out.write(withPivot);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write pivot table result", e);
            }
        }

        validateFileSize(out);

        String fileName = resolveFileName(context);
        String contentType = resolveContentType(context.getOutputFormat());

        log.info("<-- EasyExcel generate | fileName={}, size={} bytes", fileName, out.size());
        return new ReportResult(out.toByteArray(), contentType, fileName);
    }

    @Override
    public ReportResult generateMerged(List<ReportContext> contexts) {
        log.info("--> EasyExcel generateMerged | count={}", contexts.size());

        if (contexts.size() == 1) {
            return generate(contexts.getFirst());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelTypeEnum excelType = toExcelType(contexts.getFirst().getOutputFormat());

        // 每個 context 對應一個 Sheet
        try (ExcelWriter writer = EasyExcel.write(out).excelType(excelType).build()) {
            for (int i = 0; i < contexts.size(); i++) {
                ReportContext ctx = contexts.get(i);
                String sheetName = ctx.getFileName() != null
                        ? ctx.getFileName().replaceAll("\\.[^.]+$", "")
                        : "Sheet" + (i + 1);

                List<?> data = ctx.getData();
                if (data == null || data.isEmpty()) {
                    continue;
                }

                Class<?> headClass = data.getFirst().getClass();
                WriteSheet sheet = EasyExcel.writerSheet(i, sheetName)
                        .head(headClass).build();
                writer.write(data, sheet);
            }
        }

        validateFileSize(out);

        ReportContext first = contexts.getFirst();
        String fileName = resolveFileName(first);
        String contentType = resolveContentType(first.getOutputFormat());

        log.info("<-- EasyExcel generateMerged | fileName={}, size={} bytes", fileName, out.size());
        return new ReportResult(out.toByteArray(), contentType, fileName);
    }

    // ==================== Pivot Table 後處理 ====================

    /**
     * 用 POI XSSF 打開 EasyExcel 產出的 byte[]，加入 Pivot Table Sheet
     *
     * <p>Pivot Table 只記錄「引用哪個資料範圍」，不複製資料，
     * 所以即使來源 Sheet 資料量很大也不會額外占用記憶體。
     */
    private byte[] addPivotTables(byte[] excelBytes, List<PivotConfig> pivots) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            for (PivotConfig pivot : pivots) {
                addPivotTable(workbook, pivot);
            }
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            workbook.write(result);
            return result.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to add pivot table", e);
        }
    }

    private void addPivotTable(XSSFWorkbook workbook, PivotConfig pivot) {
        XSSFSheet sourceSheet = workbook.getSheet(pivot.getSourceSheet());
        if (sourceSheet == null) {
            throw new IllegalArgumentException(
                    "Pivot source sheet not found: " + pivot.getSourceSheet());
        }

        // 計算資料範圍（header 行 + 資料行）
        int lastRow = sourceSheet.getLastRowNum();
        int lastCol = sourceSheet.getRow(0).getLastCellNum() - 1;

        AreaReference dataArea = new AreaReference(
                new CellReference(0, 0),
                new CellReference(lastRow, lastCol),
                SpreadsheetVersion.EXCEL2007
        );

        // 建立目標 Sheet
        XSSFSheet pivotSheet = workbook.createSheet(pivot.getTargetSheet());

        // 建立 Pivot Table（放在 A1）
        XSSFPivotTable pivotTable = pivotSheet.createPivotTable(
                dataArea, new CellReference("A1"), sourceSheet);

        // 讀取 header 名稱，建立 name → column index 對應
        var headerRow = sourceSheet.getRow(0);

        // 設定列標籤（Row Labels）
        for (String rowLabel : pivot.getRowLabels()) {
            int colIdx = findColumnIndex(headerRow, rowLabel);
            if (colIdx >= 0) {
                pivotTable.addRowLabel(colIdx);
            }
        }

        // 設定欄標籤（Column Labels）
        for (String colLabel : pivot.getColumnLabels()) {
            int colIdx = findColumnIndex(headerRow, colLabel);
            if (colIdx >= 0) {
                pivotTable.addColLabel(colIdx);
            }
        }

        // 設定值欄位 — 直接操作 CTDataFields 避免同欄位多函數時被覆蓋
        var ctPivotTable = pivotTable.getCTPivotTableDefinition();
        CTDataFields dataFields = ctPivotTable.getDataFields();
        if (dataFields == null) {
            dataFields = ctPivotTable.addNewDataFields();
        }

        for (PivotConfig.ValueField vf : pivot.getValueFields()) {
            int colIdx = findColumnIndex(headerRow, vf.columnName());
            if (colIdx < 0) {
                continue;
            }
            var df = dataFields.addNewDataField();
            df.setFld(colIdx);
            df.setName(vf.resolveLabel());
            df.setSubtotal(toCtFunction(vf.function()));
        }
        dataFields.setCount(dataFields.getDataFieldList().size());
    }

    private org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataConsolidateFunction.Enum toCtFunction(
            PivotConfig.ConsolidateFunction function) {
        return switch (function) {
            case SUM -> org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataConsolidateFunction.SUM;
            case COUNT -> org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataConsolidateFunction.COUNT;
            case AVERAGE -> org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataConsolidateFunction.AVERAGE;
            case MAX -> org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataConsolidateFunction.MAX;
            case MIN -> org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataConsolidateFunction.MIN;
            case COUNT_NUMS -> org.openxmlformats.schemas.spreadsheetml.x2006.main.STDataConsolidateFunction.COUNT_NUMS;
        };
    }

    private int findColumnIndex(org.apache.poi.ss.usermodel.Row headerRow, String columnName) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            var cell = headerRow.getCell(i);
            if (cell != null && columnName.equals(cell.getStringCellValue())) {
                return i;
            }
        }
        log.warn("Pivot column not found: '{}', skipping", columnName);
        return -1;
    }

    private org.apache.poi.ss.usermodel.DataConsolidateFunction toPoiFunction(
            PivotConfig.ConsolidateFunction function) {
        return switch (function) {
            case SUM -> org.apache.poi.ss.usermodel.DataConsolidateFunction.SUM;
            case COUNT -> org.apache.poi.ss.usermodel.DataConsolidateFunction.COUNT;
            case AVERAGE -> org.apache.poi.ss.usermodel.DataConsolidateFunction.AVERAGE;
            case MAX -> org.apache.poi.ss.usermodel.DataConsolidateFunction.MAX;
            case MIN -> org.apache.poi.ss.usermodel.DataConsolidateFunction.MIN;
            case COUNT_NUMS -> org.apache.poi.ss.usermodel.DataConsolidateFunction.COUNT_NUMS;
        };
    }

    // ==================== 多 Sheet 模式 ====================

    private void generateMultiSheet(List<SheetData> sheets,
                                    ByteArrayOutputStream out,
                                    ExcelTypeEnum excelType) {
        try (ExcelWriter writer = EasyExcel.write(out).excelType(excelType).build()) {
            for (int i = 0; i < sheets.size(); i++) {
                SheetData sd = sheets.get(i);
                Class<?> head = sd.getHeadClass();
                if (head == null && sd.getData() != null && !sd.getData().isEmpty()) {
                    head = sd.getData().getFirst().getClass();
                }
                WriteSheet sheet = EasyExcel.writerSheet(i, sd.getSheetName())
                        .head(head).build();
                writer.write(sd.getData(), sheet);
            }
        }
    }

    // ==================== 範本填充模式 ====================

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

            if (context.getParameters() != null && !context.getParameters().isEmpty()) {
                writer.fill(context.getParameters(), sheet);
            }
            if (context.getData() != null && !context.getData().isEmpty()) {
                writer.fill(context.getData(), fillConfig, sheet);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate report from template", e);
        }
    }

    // ==================== 資料寫入模式 ====================

    private void generateWithData(ReportContext context,
                                  ByteArrayOutputStream out,
                                  ExcelTypeEnum excelType) {
        if (context.getData() == null || context.getData().isEmpty()) {
            throw new IllegalArgumentException("Data list cannot be empty for data-driven report");
        }
        Class<?> dataClass = context.getData().getFirst().getClass();

        EasyExcel.write(out, dataClass)
                .excelType(excelType)
                .sheet("Sheet1")
                .doWrite(context.getData());
    }

    // ==================== 工具方法 ====================

    private void validateFileSize(ByteArrayOutputStream out) {
        if (out.size() > MAX_FILE_SIZE) {
            throw new IllegalStateException("Report file exceeds maximum allowed size: 50MB");
        }
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
