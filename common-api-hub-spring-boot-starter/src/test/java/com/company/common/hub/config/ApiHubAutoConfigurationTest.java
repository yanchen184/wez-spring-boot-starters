package com.company.common.hub.config;

import com.company.common.hub.service.HubAuthService;
import com.company.common.hub.service.HubLogService;
import com.company.common.hub.service.HubTokenService;
import com.company.common.hub.service.IpWhitelistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiHubAutoConfiguration 測試。
 *
 * <p>使用 WebApplicationContextRunner 驗證 Bean 條件載入。
 */
@DisplayName("ApiHubAutoConfiguration 測試")
class ApiHubAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    JacksonAutoConfiguration.class,
                    ApiHubAutoConfiguration.class
            ))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:autoconfig-test;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "common.api-hub.jwt.secret-key=test-secret-key-must-be-at-least-32-chars-long!!"
            );

    @Test
    @DisplayName("enabled=true 時應載入所有 Bean")
    void shouldLoadAllBeans_whenEnabled() {
        contextRunner
                .withPropertyValues("common.api-hub.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(HubTokenService.class);
                    assertThat(context).hasSingleBean(IpWhitelistService.class);
                    assertThat(context).hasSingleBean(HubLogService.class);
                    assertThat(context).hasSingleBean(HubAuthService.class);
                    assertThat(context).hasSingleBean(PasswordEncoder.class);
                });
    }

    @Test
    @DisplayName("enabled=false 時不應載入任何 Bean")
    void shouldNotLoadBeans_whenDisabled() {
        contextRunner
                .withPropertyValues("common.api-hub.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(HubTokenService.class);
                    assertThat(context).doesNotHaveBean(HubAuthService.class);
                });
    }

    @Test
    @DisplayName("未設定 enabled 時不應載入")
    void shouldNotLoadBeans_whenPropertyNotSet() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(HubTokenService.class);
                    assertThat(context).doesNotHaveBean(HubAuthService.class);
                });
    }

    @Test
    @DisplayName("自訂 PasswordEncoder 時不應覆蓋")
    void shouldNotOverrideCustomPasswordEncoder() {
        contextRunner
                .withPropertyValues("common.api-hub.enabled=true")
                .withBean("customEncoder", PasswordEncoder.class,
                        () -> new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4))
                .run(context -> {
                    assertThat(context).hasSingleBean(PasswordEncoder.class);
                });
    }

    @Test
    @DisplayName("應正確讀取自訂屬性")
    void shouldReadCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "common.api-hub.enabled=true",
                        "common.api-hub.ip-whitelist.allow-local=false",
                        "common.api-hub.log.retention-days=30"
                )
                .run(context -> {
                    ApiHubProperties props = context.getBean(ApiHubProperties.class);
                    assertThat(props.getIpWhitelist().isAllowLocal()).isFalse();
                    assertThat(props.getLog().getRetentionDays()).isEqualTo(30);
                });
    }
}
