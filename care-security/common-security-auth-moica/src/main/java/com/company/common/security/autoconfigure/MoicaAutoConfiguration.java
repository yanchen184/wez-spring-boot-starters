package com.company.common.security.autoconfigure;

import com.company.common.security.cert.CertFactory;
import com.company.common.security.cert.CertProvider;
import com.company.common.security.cert.provider.MoicaCertProvider;
import com.company.common.security.controller.CitizenCertController;
import com.company.common.security.service.CitizenCertUserSyncService;
import com.company.common.security.service.LoginTokenService;
import com.company.common.security.service.MoicaCertService;
import com.company.common.security.repository.OrganizeRepository;
import com.company.common.security.repository.RoleRepository;
import com.company.common.security.repository.SaUserOrgRoleRepository;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.service.AuditService;
import com.company.common.security.service.AuthService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * Auto-configuration for MOICA citizen digital certificate (自然人憑證) authentication.
 *
 * 引入 jar 即自動啟用；設 care.security.citizen-cert.enabled=false 可關閉。
 *
 * Usage (without full starter):
 * <pre>
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;com.company.common&lt;/groupId&gt;
 *     &lt;artifactId&gt;common-security-core&lt;/artifactId&gt;
 *   &lt;/dependency&gt;
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;com.company.common&lt;/groupId&gt;
 *     &lt;artifactId&gt;common-security-auth-moica&lt;/artifactId&gt;
 *   &lt;/dependency&gt;
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(CareSecurityProperties.class)
@ConditionalOnProperty(prefix = "care.security.citizen-cert", name = "enabled", matchIfMissing = true)
public class MoicaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoginTokenService loginTokenService(RedisTemplate<String, Object> redisTemplate,
                                               CareSecurityProperties properties) {
        return new LoginTokenService(redisTemplate,
                properties.getCitizenCert().getChallengeExpireSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public MoicaCertService moicaCertService(ResourceLoader resourceLoader,
                                             CareSecurityProperties properties) {
        CareSecurityProperties.CitizenCert certProps = properties.getCitizenCert();
        return new MoicaCertService(resourceLoader,
                certProps.getIntermediateCertPaths(),
                certProps.getLocalCrlPaths(),
                certProps.isOcspEnabled(),
                certProps.isCrlEnabled(),
                certProps.getCrlCacheTtlHours());
    }

    @Bean
    @ConditionalOnMissingBean
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
    public MoicaCertProvider moicaCertProvider() {
        return new MoicaCertProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public CertFactory certFactory(List<CertProvider> providers) {
        return new CertFactory(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public CitizenCertController citizenCertController(LoginTokenService loginTokenService,
                                                       MoicaCertService moicaCertService,
                                                       CitizenCertUserSyncService citizenCertUserSyncService,
                                                       AuthService authService,
                                                       AuditService auditService) {
        return new CitizenCertController(loginTokenService, moicaCertService,
                citizenCertUserSyncService, authService, auditService);
    }

}
