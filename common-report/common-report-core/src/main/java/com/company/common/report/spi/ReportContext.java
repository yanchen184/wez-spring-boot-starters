package com.company.common.report.spi;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;

import java.util.HashMap;
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

    /** 額外參數 */
    private Map<String, Object> parameters = new HashMap<>();

    /** 資料集 */
    private List<?> data;

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String templatePath;
        private ReportEngineType engineType;
        private OutputFormat outputFormat;
        private String fileName;
        private Map<String, Object> parameters = new HashMap<>();
        private List<?> data;

        public Builder templatePath(String templatePath) { this.templatePath = templatePath; return this; }
        public Builder engineType(ReportEngineType engineType) { this.engineType = engineType; return this; }
        public Builder outputFormat(OutputFormat outputFormat) { this.outputFormat = outputFormat; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder parameters(Map<String, Object> parameters) { this.parameters = parameters; return this; }
        public Builder parameter(String key, Object value) { this.parameters.put(key, value); return this; }
        public Builder data(List<?> data) { this.data = data; return this; }

        public ReportContext build() {
            ReportContext ctx = new ReportContext();
            ctx.setTemplatePath(templatePath);
            ctx.setEngineType(engineType);
            ctx.setOutputFormat(outputFormat);
            ctx.setFileName(fileName);
            ctx.setParameters(parameters);
            ctx.setData(data);
            return ctx;
        }
    }
}
