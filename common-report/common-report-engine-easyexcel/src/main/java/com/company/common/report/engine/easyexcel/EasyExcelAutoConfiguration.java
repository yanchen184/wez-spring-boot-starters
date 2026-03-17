package com.company.common.report.engine.easyexcel;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * EasyExcel 引擎自動配置
 *
 * <p>當 classpath 有 EasyExcel 且 common.report.enabled=true 時自動註冊引擎。
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.alibaba.excel.EasyExcel")
@ConditionalOnProperty(prefix = "common.report", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EasyExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EasyExcelReportEngine easyExcelReportEngine() {
        return new EasyExcelReportEngine();
    }
}
