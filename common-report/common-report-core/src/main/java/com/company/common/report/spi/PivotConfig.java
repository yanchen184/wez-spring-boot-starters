package com.company.common.report.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * 樞紐分析表設定
 *
 * <p>搭配 {@link ReportContext#getPivots()} 使用，
 * 在 EasyExcel 產出的 Excel 中自動建立 Pivot Table Sheet。
 */
public class PivotConfig {

    /** 資料來源 Sheet 名稱 */
    private final String sourceSheet;

    /** 樞紐分析表放置的 Sheet 名稱 */
    private final String targetSheet;

    /** 列標籤（Row Labels）— 使用 Excel 欄位名稱 */
    private final List<String> rowLabels;

    /** 欄標籤（Column Labels） */
    private final List<String> columnLabels;

    /** 值欄位設定（Value Fields） */
    private final List<ValueField> valueFields;

    private PivotConfig(String sourceSheet, String targetSheet,
                        List<String> rowLabels, List<String> columnLabels,
                        List<ValueField> valueFields) {
        this.sourceSheet = sourceSheet;
        this.targetSheet = targetSheet;
        this.rowLabels = rowLabels;
        this.columnLabels = columnLabels;
        this.valueFields = valueFields;
    }

    public String getSourceSheet() {
        return sourceSheet;
    }

    public String getTargetSheet() {
        return targetSheet;
    }

    public List<String> getRowLabels() {
        return rowLabels;
    }

    public List<String> getColumnLabels() {
        return columnLabels;
    }

    public List<ValueField> getValueFields() {
        return valueFields;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 值欄位設定
     *
     * @param columnName   Excel 欄位名稱（header 文字）
     * @param function     彙總函數（SUM, COUNT, AVERAGE 等）
     * @param displayLabel Pivot Table 顯示的欄位名稱（null 時使用 columnName）
     */
    public record ValueField(String columnName, ConsolidateFunction function, String displayLabel) {

        public String resolveLabel() {
            return displayLabel != null ? displayLabel : columnName;
        }
    }

    /**
     * 樞紐分析表彙總函數
     */
    public enum ConsolidateFunction {
        SUM,
        COUNT,
        AVERAGE,
        MAX,
        MIN,
        COUNT_NUMS
    }

    public static class Builder {
        private String sourceSheet;
        private String targetSheet = "樞紐分析";
        private final List<String> rowLabels = new ArrayList<>();
        private final List<String> columnLabels = new ArrayList<>();
        private final List<ValueField> valueFields = new ArrayList<>();

        public Builder sourceSheet(String sourceSheet) {
            this.sourceSheet = sourceSheet;
            return this;
        }

        public Builder targetSheet(String targetSheet) {
            this.targetSheet = targetSheet;
            return this;
        }

        /** 加入列標籤欄位 */
        public Builder row(String columnName) {
            this.rowLabels.add(columnName);
            return this;
        }

        /** 加入欄標籤欄位 */
        public Builder column(String columnName) {
            this.columnLabels.add(columnName);
            return this;
        }

        /** 加入值欄位（預設 SUM，label 同欄位名） */
        public Builder value(String columnName) {
            this.valueFields.add(new ValueField(columnName, ConsolidateFunction.SUM, null));
            return this;
        }

        /** 加入值欄位（指定函數，label 同欄位名） */
        public Builder value(String columnName, ConsolidateFunction function) {
            this.valueFields.add(new ValueField(columnName, function, null));
            return this;
        }

        /** 加入值欄位（指定函數 + 自訂顯示名稱） */
        public Builder value(String columnName, ConsolidateFunction function, String displayLabel) {
            this.valueFields.add(new ValueField(columnName, function, displayLabel));
            return this;
        }

        public PivotConfig build() {
            if (sourceSheet == null || sourceSheet.isBlank()) {
                throw new IllegalArgumentException("sourceSheet is required for PivotConfig");
            }
            if (valueFields.isEmpty()) {
                throw new IllegalArgumentException("At least one value field is required for PivotConfig");
            }
            return new PivotConfig(sourceSheet, targetSheet, rowLabels, columnLabels, valueFields);
        }
    }
}
