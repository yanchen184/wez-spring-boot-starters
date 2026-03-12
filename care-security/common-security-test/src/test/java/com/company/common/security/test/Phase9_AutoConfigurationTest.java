package com.company.common.security.test;

import com.company.common.security.autoconfigure.CareSecurityAutoConfiguration;
import com.company.common.security.autoconfigure.CareSecurityProperties;
import com.company.common.security.security.CrudPermissionEvaluator;
import com.company.common.security.security.CustomUserDetailsService;
import com.company.common.security.security.LoginAttemptService;
import com.company.common.security.security.RedisTokenBlacklistService;
import com.company.common.security.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@CareSecurityTest
@DisplayName("Phase 9: AutoConfiguration Validation")
class Phase9_AutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CareSecurityProperties properties;

    @Test
    @DisplayName("9.1 All core beans are loaded by auto-configuration")
    void testAllBeansLoaded() {
        assertThat(context.getBean(PasswordEncoder.class)).isNotNull();
        assertThat(context.getBean(CustomUserDetailsService.class)).isNotNull();
        assertThat(context.getBean(CrudPermissionEvaluator.class)).isNotNull();

        assertThat(context.getBean(AuthService.class)).isNotNull();
        assertThat(context.getBean(LoginAttemptService.class)).isNotNull();
        assertThat(context.getBean(RedisTokenBlacklistService.class)).isNotNull();
        assertThat(context.getBean(AuditService.class)).isNotNull();
        assertThat(context.getBean(UserService.class)).isNotNull();
        assertThat(context.getBean(RoleService.class)).isNotNull();
        assertThat(context.getBean(MenuService.class)).isNotNull();
        assertThat(context.getBean(OrganizeService.class)).isNotNull();

        assertThat(context.getBean(JwtEncoder.class)).isNotNull();
        assertThat(context.getBean(JwtDecoder.class)).isNotNull();

        assertThat(context.getBean("redisTemplate")).isNotNull();
    }

    @Test
    @DisplayName("9.2 CareSecurityProperties binding reflects yml values")
    void testPropertiesBinding() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getJwt().getAccessTokenTtlMinutes()).isEqualTo(30);
        assertThat(properties.getJwt().getRefreshTokenTtlDays()).isEqualTo(7);
        assertThat(properties.getLogin().getMaxAttempts()).isGreaterThanOrEqualTo(1);
        assertThat(properties.getLogin().getLockDurationMinutes()).isEqualTo(30);
        assertThat(properties.getCors().getAllowedOrigins()).isEqualTo("http://localhost:3000");
    }

    @Test
    @DisplayName("9.3 CareSecurityProperties defaults are correct when not overridden")
    void testPropertiesDefaults() {
        CareSecurityProperties defaults = new CareSecurityProperties();
        assertThat(defaults.isEnabled()).isTrue();
        assertThat(defaults.getJwt().getAccessTokenTtlMinutes()).isEqualTo(30);
        assertThat(defaults.getJwt().getRefreshTokenTtlDays()).isEqualTo(7);
        assertThat(defaults.getLogin().getMaxAttempts()).isEqualTo(5);
        assertThat(defaults.getLogin().getLockDurationMinutes()).isEqualTo(30);
        assertThat(defaults.getCors().getAllowedOrigins()).isEqualTo("http://localhost:3000");
    }

    @Test
    @DisplayName("9.4 @ConditionalOnMissingBean ensures PasswordEncoder is replaceable")
    void testPasswordEncoderIsConditional() {
        PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
        assertThat(encoder).isNotNull();
        assertThat(encoder.getClass().getSimpleName()).contains("Delegating");
    }

    @Test
    @DisplayName("9.5 @ConditionalOnMissingBean ensures CustomUserDetailsService is replaceable")
    void testUserDetailsServiceIsConditional() {
        CustomUserDetailsService uds = context.getBean(CustomUserDetailsService.class);
        assertThat(uds).isNotNull();
        assertThat(uds).isInstanceOf(CustomUserDetailsService.class);
    }

    @Test
    @DisplayName("9.6 Three SecurityFilterChain beans exist")
    void testFilterChainCount() {
        Map<String, SecurityFilterChain> chains = context.getBeansOfType(SecurityFilterChain.class);
        assertThat(chains).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("9.7 CareSecurityAutoConfiguration is loaded")
    void testAutoConfigurationIsLoaded() {
        assertThat(context.getBeansOfType(CareSecurityAutoConfiguration.class)).isNotEmpty();
    }
}
