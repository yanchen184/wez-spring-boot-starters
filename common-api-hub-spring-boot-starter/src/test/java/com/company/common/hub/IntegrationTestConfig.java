package com.company.common.hub;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * 整合測試用額外 Bean 配置。
 *
 * <p>搭配 {@link TestJpaConfig}（主要 @SpringBootApplication）使用，
 * 手動注冊所有 Service / Controller / Filter Bean。
 */
@TestConfiguration
public class IntegrationTestConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public HubTokenService hubTokenService() {
        return new HubTokenService("integration-test-secret-key-at-least-32-chars!!");
    }

    @Bean
    public IpWhitelistService ipWhitelistService() {
        return new IpWhitelistService(false); // 嚴格測 IP
    }

    @Bean
    public HubLogService hubLogService(HubLogRepository hubLogRepository) {
        return new HubLogService(hubLogRepository, List.of("password", "passcode", "secret"));
    }

    @Bean
    public HubAuthService hubAuthService(HubSetRepository hubSetRepository,
                                          HubUserRepository hubUserRepository,
                                          HubUserSetRepository hubUserSetRepository,
                                          HubTokenService hubTokenService,
                                          IpWhitelistService ipWhitelistService,
                                          PasswordEncoder passwordEncoder) {
        return new HubAuthService(
                hubSetRepository, hubUserRepository, hubUserSetRepository,
                hubTokenService, ipWhitelistService, passwordEncoder
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
                                                   HubLogRepository hubLogRepository,
                                                   PasswordEncoder passwordEncoder) {
        return new HubAdminController(
                hubSetRepository, hubUserRepository, hubUserSetRepository,
                hubLogRepository, passwordEncoder
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
        return registration;
    }
}
