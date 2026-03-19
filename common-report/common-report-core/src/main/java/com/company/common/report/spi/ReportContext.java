package com.company.common.report.spi;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 報表產製上下文
 */
public class ReportContext {

    /** 範本路徑 */
    private String templatePath;

    /** 引擎類型 */
    private ReportEngineType engineType;

    /** 輸出格式 */
    private OutputFormat outputFormat;

    /** 輸出檔名 */
    private String fileName;

    /** 報表名稱（限流用，若未設定則以 fileName 作為 fallback） */
    private String reportName;

    /** 額外參數 */
    private Map<String, Object> parameters = new HashMap<>();

    /** 資料集 */
    private List<?> data;

    /** 多 Sheet 資料（EasyExcel 用） */
    private List<SheetData> sheets = new ArrayList<>();

    /** 圖片來源（xDocReport 用） */
    private Map<String, ImageSource> images = new LinkedHashMap<>();

    /** 樞紐分析表設定（EasyExcel 用） */
    private List<PivotConfig> pivots = new ArrayList<>();

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public ReportEngineType getEngineType() {
        return engineType;
    }

    public void setEngineType(ReportEngineType engineType) {
        this.engineType = engineType;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public List<?> getData() {
        return data;
    }

    public void setData(List<?> data) {
        this.data = data;
    }

    public List<SheetData> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetData> sheets) {
        this.sheets = sheets;
    }

    public Map<String, ImageSource> getImages() {
        return images;
    }

    public void setImages(Map<String, ImageSource> images) {
        this.images = images;
    }

    public List<PivotConfig> getPivots() {
        return pivots;
    }

    public void setPivots(List<PivotConfig> pivots) {
        this.pivots = pivots;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String templatePath;
        private ReportEngineType engineType;
        private OutputFormat outputFormat;
        private String fileName;
        private String reportName;
        private Map<String, Object> parameters = new HashMap<>();
        private List<?> data;
        private List<SheetData> sheets = new ArrayList<>();
        private Map<String, ImageSource> images = new LinkedHashMap<>();
        private List<PivotConfig> pivots = new ArrayList<>();

        public Builder templatePath(String templatePath) { this.templatePath = templatePath; return this; }
        public Builder engineType(ReportEngineType engineType) { this.engineType = engineType; return this; }
        public Builder outputFormat(OutputFormat outputFormat) { this.outputFormat = outputFormat; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder reportName(String reportName) { this.reportName = reportName; return this; }
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
            return this;
        }
        public Builder parameter(String key, Object value) { this.parameters.put(key, value); return this; }
        public Builder data(List<?> data) { this.data = data; return this; }

        // ===== 多 Sheet =====

        /** 加入一頁 Sheet（自動從 data 推導 headClass） */
        public Builder sheet(String sheetName, List<?> data) {
            this.sheets.add(SheetData.of(sheetName, data));
            return this;
        }

        /** 加入一頁 Sheet（指定 headClass） */
        public Builder sheet(String sheetName, List<?> data, Class<?> headClass) {
            this.sheets.add(SheetData.of(sheetName, data, headClass));
            return this;
        }

        /** 加入一頁 Sheet（完整控制） */
        public Builder sheet(SheetData sheetData) {
            this.sheets.add(sheetData);
            return this;
        }

        /** 設定所有 Sheet */
        public Builder sheets(List<SheetData> sheets) {
            this.sheets = sheets != null ? new ArrayList<>(sheets) : new ArrayList<>();
            return this;
        }

        // ===== 圖片 =====

        /** 插入圖片（byte[] 來源） */
        public Builder image(String fieldName, byte[] content) {
            this.images.put(fieldName, ImageSource.fromBytes(content));
            return this;
        }

        /** 插入圖片（byte[] 來源，指定寬高） */
        public Builder image(String fieldName, byte[] content, int width, int height) {
            this.images.put(fieldName, ImageSource.fromBytes(content, width, height));
            return this;
        }

        /** 插入圖片（檔案路徑來源） */
        public Builder imageFromFile(String fieldName, String filePath) {
            this.images.put(fieldName, ImageSource.fromFile(filePath));
            return this;
        }

        /** 插入圖片（檔案路徑來源，指定寬高） */
        public Builder imageFromFile(String fieldName, String filePath, int width, int height) {
            this.images.put(fieldName, ImageSource.fromFile(filePath, width, height));
            return this;
        }

        /** 插入圖片（ImageSource 完整控制） */
        public Builder image(String fieldName, ImageSource imageSource) {
            this.images.put(fieldName, imageSource);
            return this;
        }

        /** 設定所有圖片 */
        public Builder images(Map<String, ImageSource> images) {
            this.images = images != null ? new LinkedHashMap<>(images) : new LinkedHashMap<>();
            return this;
        }

        // ===== 樞紐分析表 =====

        /** 加入樞紐分析表 */
        public Builder pivot(PivotConfig pivotConfig) {
            this.pivots.add(pivotConfig);
            return this;
        }

        /** 設定所有樞紐分析表 */
        public Builder pivots(List<PivotConfig> pivots) {
            this.pivots = pivots != null ? new ArrayList<>(pivots) : new ArrayList<>();
            return this;
        }

        public ReportContext build() {
            ReportContext ctx = new ReportContext();
            ctx.setTemplatePath(templatePath);
            ctx.setEngineType(engineType);
            ctx.setOutputFormat(outputFormat);
            ctx.setFileName(fileName);
            ctx.setReportName(reportName);
            ctx.setParameters(parameters);
            ctx.setData(data);
            ctx.setSheets(sheets);
            ctx.setImages(images);
            ctx.setPivots(pivots);
            return ctx;
        }
    }
}
