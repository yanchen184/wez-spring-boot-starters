package com.company.common.report.spi;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;

/**
 * 報表引擎 SPI 介面
 *
 * <p>每個引擎實作此介面，由 AutoConfiguration 自動註冊。
 * ReportService 根據 {@link ReportEngineType} 派發到對應引擎。
 */
public interface ReportEngine {

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
}
