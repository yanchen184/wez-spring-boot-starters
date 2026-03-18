package com.company.common.security.autoconfigure;

import com.company.common.security.controller.CaptchaController;
import com.company.common.security.service.CaptchaService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Auto-configuration for captcha (圖形驗證碼) support.
 *
 * 引入 jar 即自動啟用；設 care.security.captcha.enabled=false 可關閉。
 */
@AutoConfiguration
@EnableConfigurationProperties(CareSecurityProperties.class)
@ConditionalOnProperty(prefix = "care.security.captcha", name = "enabled", matchIfMissing = true)
public class CaptchaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CaptchaService captchaService(RedisTemplate<String, Object> redisTemplate,
                                          CareSecurityProperties properties) {
        return new CaptchaService(redisTemplate, properties.getCaptcha());
    }

    @Bean
    @ConditionalOnMissingBean
    public CaptchaController captchaController(CaptchaService captchaService) {
        return new CaptchaController(captchaService);
    }
}
