package com.company.common.hub.config;

import com.company.common.hub.controller.HubAdminController;
import com.company.common.hub.controller.HubTokenController;
import com.company.common.hub.filter.HubAuthenticationFilter;
import com.company.common.hub.repository.HubLogRepository;
import com.company.common.hub.repository.HubSetRepository;
import com.company.common.hub.repository.HubUserRepository;
import com.company.common.hub.repository.HubUserSetRepository;
import com.company.common.hub.service.HubAuthService;
import com.company.common.hub.service.HubLogService;
import com.company.common.hub.service.HubTokenService;
import com.company.common.hub.service.IpWhitelistService;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;  // 用在 method 內部建立 BCryptPasswordEncoder

/**
 * API Hub 自動配置。
 *
 * <p>當 {@code common.api-hub.enabled=true} 時自動註冊所有 Bean。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "common.api-hub", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ApiHubProperties.class)
@EntityScan(basePackages = "com.company.common.hub.entity")
@EnableJpaRepositories(basePackages = "com.company.common.hub.repository")
public class ApiHubAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HubTokenService hubTokenService(ApiHubProperties properties) {
        return new HubTokenService(properties.getJwt().getSecretKey());
    }

    @Bean
    @ConditionalOnMissingBean
    public IpWhitelistService ipWhitelistService(ApiHubProperties properties) {
        return new IpWhitelistService(properties.getIpWhitelist().isAllowLocal());
    }

    @Bean
    @ConditionalOnMissingBean
    public HubLogService hubLogService(HubLogRepository hubLogRepository,
                                        ApiHubProperties properties) {
        return new HubLogService(hubLogRepository, properties.getLog().getMaskFields());
    }

    @Bean
    @ConditionalOnMissingBean
    public HubAuthService hubAuthService(HubSetRepository hubSetRepository,
                                          HubUserRepository hubUserRepository,
                                          HubUserSetRepository hubUserSetRepository,
                                          HubTokenService hubTokenService,
                                          IpWhitelistService ipWhitelistService) {
        // Hub 用獨立的 BCryptPasswordEncoder，不干擾 care-security 的 DelegatingPasswordEncoder
        PasswordEncoder hubPasswordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        return new HubAuthService(
                hubSetRepository, hubUserRepository, hubUserSetRepository,
                hubTokenService, ipWhitelistService, hubPasswordEncoder
        );
    }

    @Bean
    public HubTokenController hubTokenController(HubAuthService hubAuthService,
                                                   HubTokenService hubTokenService,
                                                   HubLogService hubLogService) {
        return new HubTokenController(hubAuthService, hubTokenService, hubLogService);
    }

    @Bean
    public HubAdminController hubAdminController(HubSetRepository hubSetRepository,
                                                   HubUserRepository hubUserRepository,
                                                   HubUserSetRepository hubUserSetRepository,
                                                   HubLogRepository hubLogRepository) {
        PasswordEncoder hubPasswordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        return new HubAdminController(
                hubSetRepository, hubUserRepository, hubUserSetRepository,
                hubLogRepository, hubPasswordEncoder
        );
    }

    @Bean
    public FilterRegistrationBean<HubAuthenticationFilter> hubAuthenticationFilter(
            HubAuthService hubAuthService,
            HubLogService hubLogService,
            HubSetRepository hubSetRepository,
            ObjectMapper objectMapper) {

        FilterRegistrationBean<HubAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(
                new HubAuthenticationFilter(hubAuthService, hubLogService, hubSetRepository, objectMapper)
        );
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        registration.setName("hubAuthenticationFilter");
        return registration;
    }
}
