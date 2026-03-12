package com.company.common.response.config;

import com.company.common.response.handler.GlobalExceptionHandler;
import com.company.common.response.handler.GlobalResponseAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/**
 * Response 模組自動配置
 *
 * @author Platform Team
 * @version 1.1.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "common.response", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ResponseProperties.class)
public class ResponseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ResponseAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler(ResponseProperties properties) {
        log.info("[Common-Response] Registering GlobalExceptionHandler (exclude-paths={})", properties.getExcludePaths());
        return new GlobalExceptionHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean(GlobalResponseAdvice.class)
    @ConditionalOnClass(JsonMapper.class)
    public GlobalResponseAdvice globalResponseAdvice(ResponseProperties properties, JsonMapper jsonMapper) {
        log.info("[Common-Response] Registering GlobalResponseAdvice");
        return new GlobalResponseAdvice(properties, jsonMapper);
    }
}
