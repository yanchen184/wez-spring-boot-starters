package com.company.common.report.entity;

import com.company.common.jpa.entity.AuditableEntity;
import com.company.common.report.enums.ReportStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * 報表產製記錄
 */
@Entity
@Table(name = "REPORT_LOG", indexes = {
        @Index(name = "idx_report_log_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_report_log_status_created", columnList = "status, created_date")
})
public class ReportLog extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 下載用 UUID */
    @Column(nullable = false, unique = true)
    private String uuid;

    /** 報表名稱 */
    @Column(nullable = false)
    private String reportName;

    /** 檔案名稱 */
    private String fileName;

    /** MIME type */
    private String contentType;

    /** 產製狀態 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    /** 錯誤訊息 */
    @Column(length = 4000)
    private String errorMessage;

    /** 開始時間 */
    private LocalDateTime startTime;

    /** 結束時間 */
    private LocalDateTime endTime;

    /** 樂觀鎖版本號 */
    @Version
    private Long version;

    /** 檔案 BLOB（延遲載入） */
    @OneToOne(mappedBy = "reportLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReportLogBlob blob;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public ReportLogBlob getBlob() {
        return blob;
    }

    public void setBlob(ReportLogBlob blob) {
        this.blob = blob;
    }
}
