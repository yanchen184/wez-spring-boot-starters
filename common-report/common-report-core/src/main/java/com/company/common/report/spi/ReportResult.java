package com.company.common.report.spi;

/**
 * 報表產製結果
 */
public class ReportResult {

    /** 檔案內容 */
    private final byte[] content;

    /** MIME type */
    private final String contentType;

    /** 檔案名稱 */
    private final String fileName;

    public ReportResult(byte[] content, String contentType, String fileName) {
        this.content = content;
        this.contentType = contentType;
        this.fileName = fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }
}
