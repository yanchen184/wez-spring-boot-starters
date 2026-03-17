package com.company.common.report.entity;

import com.company.common.jpa.entity.AuditableEntity;
import com.company.common.report.enums.ReportEngineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 報表項目定義
 */
@Entity
@Table(name = "REPORT_ITEM")
public class ReportItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 報表名稱 */
    @Column(nullable = false)
    private String name;

    /** 範本路徑 */
    private String templatePath;

    /** 使用的引擎 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportEngineType engineType;

    /** 預設輸出格式 */
    private String outputFormat;

    /** 報表描述 */
    @Column(length = 1000)
    private String description;

    /** 是否啟用 */
    private boolean enabled = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
