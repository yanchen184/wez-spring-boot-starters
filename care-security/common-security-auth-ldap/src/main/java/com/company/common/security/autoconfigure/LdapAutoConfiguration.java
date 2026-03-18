package com.company.common.security.autoconfigure;

import com.company.common.security.controller.LdapController;
import com.company.common.security.repository.OrganizeRepository;
import com.company.common.security.repository.RoleRepository;
import com.company.common.security.repository.SaUserOrgRoleRepository;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.security.LdapAuthenticationProvider;
import com.company.common.security.service.LdapUserSyncService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for LDAP authentication.
 *
 * 引入 jar 即自動啟用；設 care.security.ldap.enabled=false 可關閉。
 */
@AutoConfiguration
@EnableConfigurationProperties(CareSecurityProperties.class)
@ConditionalOnProperty(prefix = "care.security.ldap", name = "enabled", matchIfMissing = true)
public class LdapAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LdapAuthenticationProvider ldapAuthenticationProvider(CareSecurityProperties properties) {
        CareSecurityProperties.Ldap ldap = properties.getLdap();
        return new LdapAuthenticationProvider(
                ldap.getUrl(), ldap.getBaseDn(), ldap.getUserSearchFilter(),
                ldap.getBindDn(), ldap.getBindPassword(),
                ldap.getDisplayNameAttr(), ldap.getEmailAttr());
    }

    @Bean
    @ConditionalOnMissingBean
    public LdapUserSyncService ldapUserSyncService(SaUserRepository saUserRepository,
                                                    RoleRepository roleRepository,
                                                    SaUserOrgRoleRepository saUserOrgRoleRepository,
                                                    OrganizeRepository organizeRepository,
                                                    CareSecurityProperties properties) {
        return new LdapUserSyncService(saUserRepository, roleRepository,
                saUserOrgRoleRepository, organizeRepository,
                properties.getLdap().getDefaultRoles());
    }

    @Bean
    @ConditionalOnMissingBean
    public LdapController ldapController(ObjectProvider<LdapAuthenticationProvider> ldapAuthProvider) {
        return new LdapController(ldapAuthProvider);
    }

}
