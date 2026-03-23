package com.company.common.report.spi;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;

import java.util.List;

/**
 * 報表引擎 SPI 介面
 *
 * <p>每個引擎實作此介面，由 AutoConfiguration 自動註冊。
 * ReportService 根據 {@link ReportEngineType} 派發到對應引擎。
 */
public interface ReportEngine {

    /** 報表產製結果的檔案大小上限（50 MB） */
    long MAX_FILE_SIZE = 50L * 1024 * 1024;

    /**
     * 回傳此引擎的類型
     */
    ReportEngineType getType();

    /**
     * 產製報表
     *
     * @param context 產製上下文（範本路徑、參數、資料等）
     * @return 產製結果（檔案內容、ContentType、檔名）
     */
    ReportResult generate(ReportContext context);

    /**
     * 此引擎是否支援指定的輸出格式
     */
    boolean supports(OutputFormat format);

    /**
     * 合併多個 context 產製一份報表
     *
     * <p>預設實作：單一 context 直接委派 generate()，多個 context 拋出不支援例外。
     * 引擎可 override 此方法實作合併邏輯（例如 EasyExcel 每個 context 一個 Sheet）。
     *
     * @param contexts 多個產製上下文（至少一個）
     * @return 合併後的產製結果
     */
    default ReportResult generateMerged(List<ReportContext> contexts) {
        if (contexts.size() == 1) {
            return generate(contexts.getFirst());
        }
        throw new UnsupportedOperationException(
                "Engine " + getType() + " does not support merged generation");
    }
}
