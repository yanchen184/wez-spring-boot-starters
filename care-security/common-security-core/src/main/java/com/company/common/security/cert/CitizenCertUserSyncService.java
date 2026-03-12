package com.company.common.security.cert;

import com.company.common.security.entity.Organize;
import com.company.common.security.entity.Role;
import com.company.common.security.entity.SaUser;
import com.company.common.security.entity.SaUserOrgRole;
import com.company.common.security.repository.OrganizeRepository;
import com.company.common.security.repository.RoleRepository;
import com.company.common.security.repository.SaUserOrgRoleRepository;
import com.company.common.security.repository.SaUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Synchronizes citizen certificate user information to the local database.
 * On first cert login, creates a local user record with citizenId as username.
 * On subsequent logins, returns the existing user.
 */
public class CitizenCertUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(CitizenCertUserSyncService.class);

    private final SaUserRepository saUserRepository;
    private final RoleRepository roleRepository;
    private final SaUserOrgRoleRepository saUserOrgRoleRepository;
    private final OrganizeRepository organizeRepository;
    private final List<String> defaultRoles;

    public CitizenCertUserSyncService(SaUserRepository saUserRepository,
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
     * Sync citizen cert user to local DB. Creates if not exists by citizenId.
     */
    @Transactional
    public SaUser syncUser(String citizenId, String displayName) {
        return saUserRepository.findByCitizenId(citizenId)
                .map(existing -> updateExistingUser(existing, displayName))
                .orElseGet(() -> createNewUser(citizenId, displayName));
    }

    private SaUser updateExistingUser(SaUser user, String displayName) {
        boolean changed = false;

        if (displayName != null && !displayName.equals(user.getCname())) {
            user.setCname(displayName);
            changed = true;
        }
        if (!"CITIZEN_CERT".equals(user.getAuthSource())) {
            user.setAuthSource("CITIZEN_CERT");
            changed = true;
        }

        if (changed) {
            user = saUserRepository.save(user);
            log.info("Updated citizen cert user in local DB: {}", user.getCitizenId());
        }
        return user;
    }

    private SaUser createNewUser(String citizenId, String displayName) {
        SaUser user = new SaUser();
        user.setUsername(citizenId);
        user.setPassword("{noop}CERT_MANAGED");
        user.setPasswordSalt("");
        user.setCname(displayName);
        user.setCitizenId(citizenId);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setAccountExpired(false);
        user.setPasswordExpired(false);
        user.setAuthSource("CITIZEN_CERT");
        user = saUserRepository.save(user);

        assignDefaultRoles(user);

        log.info("Created new citizen cert user in local DB: {} with default roles: {}", citizenId, defaultRoles);
        return user;
    }

    private void assignDefaultRoles(SaUser user) {
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
                    () -> log.warn("Default cert role not found: {}", roleAuthority)
            );
        }
    }
}
