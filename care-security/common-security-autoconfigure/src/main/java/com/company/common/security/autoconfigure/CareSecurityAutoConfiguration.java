package com.company.common.security.autoconfigure;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.company.common.security.config.AuthorizationServerConfig;
import com.company.common.security.config.CorsConfig;
import com.company.common.security.config.OpenApiConfig;
import com.company.common.security.config.PasswordEncoderConfig;
import com.company.common.security.config.RedisConfig;
import com.company.common.security.config.SecurityConfig;
import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.AuditLogRepository;
import com.company.common.security.repository.LoginHistoryRepository;
import com.company.common.security.repository.MenuRepository;
import com.company.common.security.repository.OrganizeRepository;
import com.company.common.security.repository.PermRepository;
import com.company.common.security.repository.PwdHistoryRepository;
import com.company.common.security.repository.RolePermsRepository;
import com.company.common.security.repository.RoleRepository;
import com.company.common.security.repository.SaUserOrgRoleRepository;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.security.CrudPermissionEvaluator;
import com.company.common.security.security.CustomUserDetailsService;
import com.company.common.security.security.JwtTokenCustomizer;
import com.company.common.security.security.LdapAuthenticationProvider;
import com.company.common.security.security.LoginAttemptService;
import com.company.common.security.security.RedisTokenBlacklistService;
import com.company.common.security.captcha.CaptchaController;
import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.cert.CertChallengeService;
import com.company.common.security.cert.CertVerificationService;
import com.company.common.security.cert.CitizenCertController;
import com.company.common.security.cert.CitizenCertUserSyncService;
import com.company.common.security.otp.OtpController;
import com.company.common.security.otp.OtpService;
import com.company.common.security.otp.TotpService;
import com.company.common.security.service.AuditService;
import com.company.common.security.service.AuthService;
import com.company.common.security.service.LdapUserSyncService;
import com.company.common.security.service.MenuService;
import com.company.common.security.service.OrgRoleService;
import com.company.common.security.service.OrganizeService;
import com.company.common.security.service.PermService;
import com.company.common.security.service.RoleService;
import com.company.common.security.service.UserService;
import com.company.common.security.service.PasswordHistoryService;
import com.company.common.security.password.PasswordPolicyProvider;
import com.company.common.security.password.PasswordPolicyService;
import com.company.common.security.password.YamlPasswordPolicyProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.function.Supplier;

@AutoConfiguration
@ConditionalOnProperty(prefix = "care.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CareSecurityProperties.class)
@ComponentScan(basePackages = {"com.company.common.security.controller", "com.company.common.security.exception"})
@EntityScan(basePackages = "com.company.common.security.entity")
@EnableJpaRepositories(basePackages = "com.company.common.security.repository")
public class CareSecurityAutoConfiguration {

    // ===== Config Beans (replacing new XxxConfig() anti-pattern) =====

    @Bean
    @ConditionalOnMissingBean(OpenApiConfig.class)
    @ConditionalOnClass(name = "org.springdoc.core.models.GroupedOpenApi")
    public OpenApiConfig openApiConfig() {
        return new OpenApiConfig();
    }

    @Bean
    @ConditionalOnMissingBean(RedisConfig.class)
    public RedisConfig redisConfig() {
        return new RedisConfig();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoderConfig.class)
    public PasswordEncoderConfig passwordEncoderConfig() {
        return new PasswordEncoderConfig();
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationServerConfig.class)
    public AuthorizationServerConfig authorizationServerConfig() {
        return new AuthorizationServerConfig();
    }

    // ===== Redis =====

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                        RedisConfig redisConfig) {
        return redisConfig.redisTemplate(connectionFactory);
    }

    // ===== Password Encoder =====

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder(PasswordEncoderConfig passwordEncoderConfig) {
        return passwordEncoderConfig.passwordEncoder();
    }

    // ===== Security Core Beans =====

    @Bean
    @ConditionalOnMissingBean
    public CustomUserDetailsService customUserDetailsService(SaUserRepository saUserRepository,
                                                              PermRepository permRepository) {
        return new CustomUserDetailsService(saUserRepository, permRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public CrudPermissionEvaluator crudPermissionEvaluator(CustomUserDetailsService userDetailsService) {
        return new CrudPermissionEvaluator(userDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenCustomizer jwtTokenCustomizer(CustomUserDetailsService userDetailsService) {
        return new JwtTokenCustomizer(userDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoginAttemptService loginAttemptService(SaUserRepository saUserRepository,
                                                    CareSecurityProperties properties) {
        return new LoginAttemptService(saUserRepository,
                properties.getLogin().getMaxAttempts(),
                properties.getLogin().getLockDurationMinutes());
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisTokenBlacklistService redisTokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        return new RedisTokenBlacklistService(redisTemplate);
    }

    // ===== Entity Factory =====

    @Bean
    @ConditionalOnMissingBean(name = "saUserFactory")
    public Supplier<SaUser> saUserFactory() {
        return SaUser::new;
    }

    // ===== LDAP =====

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.ldap", name = "enabled", havingValue = "true")
    public LdapAuthenticationProvider ldapAuthenticationProvider(CareSecurityProperties properties) {
        CareSecurityProperties.Ldap ldap = properties.getLdap();
        return new LdapAuthenticationProvider(
                ldap.getUrl(), ldap.getBaseDn(), ldap.getUserSearchFilter(),
                ldap.getBindDn(), ldap.getBindPassword(),
                ldap.getDisplayNameAttr(), ldap.getEmailAttr());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.ldap", name = "enabled", havingValue = "true")
    public LdapUserSyncService ldapUserSyncService(SaUserRepository saUserRepository,
                                                    RoleRepository roleRepository,
                                                    SaUserOrgRoleRepository saUserOrgRoleRepository,
                                                    OrganizeRepository organizeRepository,
                                                    CareSecurityProperties properties) {
        return new LdapUserSyncService(saUserRepository, roleRepository,
                saUserOrgRoleRepository, organizeRepository, properties.getLdap().getDefaultRoles());
    }

    // ===== CAPTCHA =====

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.captcha", name = "enabled", havingValue = "true")
    public CaptchaService captchaService(RedisTemplate<String, Object> redisTemplate,
                                          CareSecurityProperties properties) {
        CareSecurityProperties.Captcha captcha = properties.getCaptcha();
        return new CaptchaService(redisTemplate, captcha.getLength(), captcha.getExpireSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.captcha", name = "enabled", havingValue = "true")
    public CaptchaController captchaController(CaptchaService captchaService) {
        return new CaptchaController(captchaService);
    }

    // ===== OTP =====

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.otp", name = "enabled", havingValue = "true")
    public TotpService totpService(CareSecurityProperties properties) {
        return new TotpService(properties.getOtp().getAllowedSkew());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.otp", name = "enabled", havingValue = "true")
    public OtpService otpService(TotpService totpService, SaUserRepository saUserRepository,
                                  CareSecurityProperties properties) {
        return new OtpService(totpService, saUserRepository, properties.getOtp().getIssuer());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.otp", name = "enabled", havingValue = "true")
    public OtpController otpController(OtpService otpService, AuthService authService) {
        return new OtpController(otpService, authService);
    }

    // ===== Citizen Certificate =====

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.citizen-cert", name = "enabled", havingValue = "true")
    public CertChallengeService certChallengeService(RedisTemplate<String, Object> redisTemplate,
                                                      CareSecurityProperties properties) {
        return new CertChallengeService(redisTemplate,
                properties.getCitizenCert().getChallengeExpireSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.citizen-cert", name = "enabled", havingValue = "true")
    public CertVerificationService certVerificationService() {
        return new CertVerificationService();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.citizen-cert", name = "enabled", havingValue = "true")
    public CitizenCertUserSyncService citizenCertUserSyncService(SaUserRepository saUserRepository,
                                                                   RoleRepository roleRepository,
                                                                   SaUserOrgRoleRepository saUserOrgRoleRepository,
                                                                   OrganizeRepository organizeRepository,
                                                                   CareSecurityProperties properties) {
        return new CitizenCertUserSyncService(saUserRepository, roleRepository,
                saUserOrgRoleRepository, organizeRepository,
                properties.getCitizenCert().getDefaultRoles());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "care.security.citizen-cert", name = "enabled", havingValue = "true")
    public CitizenCertController citizenCertController(CertChallengeService certChallengeService,
                                                        CertVerificationService certVerificationService,
                                                        CitizenCertUserSyncService citizenCertUserSyncService,
                                                        AuthService authService,
                                                        AuditService auditService) {
        return new CitizenCertController(certChallengeService, certVerificationService,
                citizenCertUserSyncService, authService, auditService);
    }

    // ===== Services =====

    @Bean
    @ConditionalOnMissingBean
    public AuditService auditService(AuditLogRepository auditLogRepository,
                                      LoginHistoryRepository loginHistoryRepository) {
        return new AuditService(auditLogRepository, loginHistoryRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthService authService(CustomUserDetailsService userDetailsService,
                                    PasswordEncoder passwordEncoder,
                                    JwtEncoder jwtEncoder,
                                    JwtDecoder jwtDecoder,
                                    RedisTokenBlacklistService blacklistService,
                                    LoginAttemptService loginAttemptService,
                                    AuditService auditService,
                                    SaUserRepository saUserRepository,
                                    PasswordHistoryService passwordHistoryService,
                                    org.springframework.beans.factory.ObjectProvider<LdapAuthenticationProvider> ldapAuthProvider,
                                    org.springframework.beans.factory.ObjectProvider<LdapUserSyncService> ldapUserSyncService,
                                    org.springframework.beans.factory.ObjectProvider<OtpService> otpServiceProvider,
                                    org.springframework.beans.factory.ObjectProvider<CaptchaService> captchaServiceProvider,
                                    CareSecurityProperties properties) {
        return new AuthService(userDetailsService, passwordEncoder, jwtEncoder, jwtDecoder,
                blacklistService, loginAttemptService, auditService, saUserRepository, passwordHistoryService,
                ldapAuthProvider.getIfAvailable(),
                ldapUserSyncService.getIfAvailable(),
                otpServiceProvider.getIfAvailable(),
                captchaServiceProvider.getIfAvailable(),
                properties.getJwt().getAccessTokenTtlMinutes(),
                properties.getJwt().getRefreshTokenTtlDays());
    }

    @Bean
    @ConditionalOnMissingBean
    public UserService userService(SaUserRepository saUserRepository,
                                    RoleRepository roleRepository,
                                    OrganizeRepository organizeRepository,
                                    SaUserOrgRoleRepository saUserOrgRoleRepository,
                                    PasswordEncoder passwordEncoder,
                                    AuditService auditService,
                                    Supplier<? extends SaUser> saUserFactory) {
        return new UserService(saUserRepository, roleRepository, organizeRepository,
                saUserOrgRoleRepository, passwordEncoder, auditService, saUserFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoleService roleService(RoleRepository roleRepository,
                                    PermRepository permRepository,
                                    RolePermsRepository rolePermsRepository,
                                    AuditService auditService) {
        return new RoleService(roleRepository, permRepository, rolePermsRepository, auditService);
    }

    @Bean
    @ConditionalOnMissingBean
    public MenuService menuService(MenuRepository menuRepository, PermRepository permRepository) {
        return new MenuService(menuRepository, permRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermService permService(PermRepository permRepository,
                                   MenuRepository menuRepository,
                                   RolePermsRepository rolePermsRepository) {
        return new PermService(permRepository, menuRepository, rolePermsRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public OrganizeService organizeService(OrganizeRepository organizeRepository) {
        return new OrganizeService(organizeRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public OrgRoleService orgRoleService(SaUserOrgRoleRepository saUserOrgRoleRepository) {
        return new OrgRoleService(saUserOrgRoleRepository);
    }

    // ===== Password Policy System =====

    @Bean
    @ConditionalOnMissingBean
    public YamlPasswordPolicyProvider yamlPasswordPolicyProvider(CareSecurityProperties properties) {
        return new YamlPasswordPolicyProvider(properties.getPassword());
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordPolicyService passwordPolicyService(List<PasswordPolicyProvider> providers) {
        return new PasswordPolicyService(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordHistoryService passwordHistoryService(PwdHistoryRepository pwdHistoryRepository,
                                                          PasswordEncoder passwordEncoder) {
        return new PasswordHistoryService(pwdHistoryRepository, passwordEncoder);
    }

    // ===== OAuth2 Authorization Server =====

    @Configuration
    static class AuthServerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RegisteredClientRepository registeredClientRepository(AuthorizationServerConfig authorizationServerConfig) {
            return authorizationServerConfig.registeredClientRepository();
        }

        @Bean
        @ConditionalOnMissingBean(JWKSource.class)
        public JWKSource<SecurityContext> jwkSource(AuthorizationServerConfig authorizationServerConfig,
                                                     CareSecurityProperties properties) {
            return authorizationServerConfig.jwkSource(properties.getJwt().getKeystorePath());
        }

        @Bean
        @ConditionalOnMissingBean(JwtDecoder.class)
        public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource,
                                      AuthorizationServerConfig authorizationServerConfig) {
            return authorizationServerConfig.jwtDecoder(jwkSource);
        }

        @Bean
        @ConditionalOnMissingBean(JwtEncoder.class)
        public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
            return new NimbusJwtEncoder(jwkSource);
        }

        @Bean
        @ConditionalOnMissingBean
        public AuthorizationServerSettings authorizationServerSettings(AuthorizationServerConfig authorizationServerConfig) {
            return authorizationServerConfig.authorizationServerSettings();
        }

        @Bean
        @ConditionalOnMissingBean(OAuth2TokenCustomizer.class)
        public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(JwtTokenCustomizer jwtTokenCustomizer,
                                                                          AuthorizationServerConfig authorizationServerConfig) {
            return authorizationServerConfig.tokenCustomizer(jwtTokenCustomizer);
        }
    }

    // ===== CORS =====

    @Bean
    @ConditionalOnMissingBean(CorsConfig.class)
    public CorsConfig corsConfig(CareSecurityProperties properties) {
        return new CorsConfig(properties.getCors().getAllowedOrigins());
    }

    @Bean
    @ConditionalOnMissingBean(UrlBasedCorsConfigurationSource.class)
    public UrlBasedCorsConfigurationSource corsConfigurationSource(CorsConfig corsConfig) {
        return corsConfig.corsConfigurationSource();
    }

    // ===== Security Filter Chains =====

    @Bean
    @ConditionalOnMissingBean(SecurityConfig.class)
    public SecurityConfig securityConfig(UrlBasedCorsConfigurationSource corsConfigurationSource,
                                          CrudPermissionEvaluator crudPermissionEvaluator) {
        return new SecurityConfig(corsConfigurationSource, crudPermissionEvaluator);
    }

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityFilterChainConfiguration {

        @Bean
        @Order(1)
        public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http,
                                                                    SecurityConfig securityConfig) {
            return securityConfig.authorizationServerFilterChain(http);
        }

        @Bean
        @Order(2)
        public SecurityFilterChain resourceServerFilterChain(HttpSecurity http,
                                                               SecurityConfig securityConfig) {
            return securityConfig.resourceServerFilterChain(http);
        }

        @Bean
        @Order(3)
        public SecurityFilterChain defaultFilterChain(HttpSecurity http,
                                                       SecurityConfig securityConfig) {
            return securityConfig.defaultFilterChain(http);
        }

        @Bean
        public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(SecurityConfig securityConfig) {
            return securityConfig.methodSecurityExpressionHandler();
        }
    }
}
