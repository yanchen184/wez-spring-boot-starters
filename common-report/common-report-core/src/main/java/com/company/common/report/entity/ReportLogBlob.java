package com.company.common.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * 報表檔案 BLOB（獨立表，避免查詢 ReportLog 時載入大型二進位資料）
 */
@Entity
@Table(name = "REPORT_LOG_BLOB")
public class ReportLogBlob {

    /** 跟 ReportLog 共用 ID */
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private ReportLog reportLog;

    /** 檔案二進位內容 */
    @Lob
    private byte[] fileBlob;

    /** 錯誤日誌 */
    @Column(length = 4000)
    private String errorLog;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReportLog getReportLog() {
        return reportLog;
    }

    public void setReportLog(ReportLog reportLog) {
        this.reportLog = reportLog;
    }

    public byte[] getFileBlob() {
        return fileBlob;
    }

    public void setFileBlob(byte[] fileBlob) {
        this.fileBlob = fileBlob;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }
}
