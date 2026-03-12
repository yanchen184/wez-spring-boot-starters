package com.company.common.security.service;

import com.company.common.security.entity.Organize;
import com.company.common.security.entity.Role;
import com.company.common.security.entity.SaUser;
import com.company.common.security.entity.SaUserOrgRole;
import com.company.common.security.repository.OrganizeRepository;
import com.company.common.security.repository.RoleRepository;
import com.company.common.security.repository.SaUserOrgRoleRepository;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.security.LdapAuthenticationProvider.LdapUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Synchronizes LDAP user information to the local database.
 * On first LDAP login, creates a local user record.
 * On subsequent logins, updates display name and email from LDAP.
 */
public class LdapUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(LdapUserSyncService.class);

    private final SaUserRepository saUserRepository;
    private final RoleRepository roleRepository;
    private final SaUserOrgRoleRepository saUserOrgRoleRepository;
    private final OrganizeRepository organizeRepository;
    private final List<String> defaultRoles;

    public LdapUserSyncService(SaUserRepository saUserRepository,
                               RoleRepository roleRepository,
                               SaUserOrgRoleRepository saUserOrgRoleRepository,
                               OrganizeRepository organizeRepository,
                               List<String> defaultRoles) {
        this.saUserRepository = saUserRepository;
        this.roleRepository = roleRepository;
        this.saUserOrgRoleRepository = saUserOrgRoleRepository;
        this.organizeRepository = organizeRepository;
        this.defaultRoles = defaultRoles;
    }

    /**
     * Sync LDAP user to local DB. Creates if not exists, updates if exists.
     */
    @Transactional
    public SaUser syncUser(LdapUserInfo ldapUser) {
        return saUserRepository.findByUsername(ldapUser.username())
                .map(existing -> updateExistingUser(existing, ldapUser))
                .orElseGet(() -> createNewUser(ldapUser));
    }

    private SaUser updateExistingUser(SaUser user, LdapUserInfo ldapUser) {
        boolean changed = false;

        if (ldapUser.displayName() != null && !ldapUser.displayName().equals(user.getCname())) {
            user.setCname(ldapUser.displayName());
            changed = true;
        }
        if (ldapUser.email() != null && !ldapUser.email().equals(user.getEmail())) {
            user.setEmail(ldapUser.email());
            changed = true;
        }
        // Ensure authSource is LDAP
        if (!"LDAP".equals(user.getAuthSource())) {
            user.setAuthSource("LDAP");
            changed = true;
        }

        if (changed) {
            user = saUserRepository.save(user);
            log.info("Updated LDAP user in local DB: {}", ldapUser.username());
        }
        return user;
    }

    private SaUser createNewUser(LdapUserInfo ldapUser) {
        SaUser user = new SaUser();
        user.setUsername(ldapUser.username());
        user.setPassword("{noop}LDAP_MANAGED");  // Placeholder, never used for auth
        user.setPasswordSalt("");                  // Not-null column, empty for LDAP users
        user.setCname(ldapUser.displayName());
        user.setEmail(ldapUser.email());
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setAccountExpired(false);
        user.setPasswordExpired(false);
        user.setAuthSource("LDAP");
        user = saUserRepository.save(user);

        // Assign default roles
        assignDefaultRoles(user);

        log.info("Created new LDAP user in local DB: {} with default roles: {}", ldapUser.username(), defaultRoles);
        return user;
    }

    private void assignDefaultRoles(SaUser user) {
        // Some DB schemas require ORGANIZE_ID to be NOT NULL, use first org as fallback
        Organize defaultOrg = organizeRepository.findById(1L).orElse(null);

        for (String roleAuthority : defaultRoles) {
            roleRepository.findByAuthority(roleAuthority).ifPresentOrElse(
                    role -> {
                        SaUserOrgRole userOrgRole = new SaUserOrgRole();
                        userOrgRole.setSaUser(user);
                        userOrgRole.setRole(role);
                        userOrgRole.setOrganize(defaultOrg);
                        saUserOrgRoleRepository.save(userOrgRole);
                    },
                    () -> log.warn("Default LDAP role not found: {}", roleAuthority)
            );
        }
    }
}
