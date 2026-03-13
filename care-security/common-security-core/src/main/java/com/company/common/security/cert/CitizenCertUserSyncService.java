package com.company.common.security.cert;

import com.company.common.security.cert.exception.MoicaUserNotFoundException;
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
import java.util.Optional;

/**
 * Synchronizes citizen certificate user information to the local database.
 * <p>
 * Supports two lookup strategies:
 * 1. By citizenId (legacy, backward compatible)
 * 2. By cname (Subject CN) + last4IDNO (from certificate extension OID 2.16.886.1.100.1.1)
 * <p>
 * On first cert login, creates a local user record.
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
     * Sync citizen cert user to local DB by citizenId. Creates if not exists.
     * (Legacy method, kept for backward compatibility)
     */
    @Transactional
    public SaUser syncUser(String citizenId, String displayName) {
        return saUserRepository.findByCitizenId(citizenId)
                .map(existing -> updateExistingUser(existing, displayName))
                .orElseGet(() -> createNewUser(citizenId, displayName));
    }

    /**
     * Lookup user by cname (Subject CN) and last4IDNO (from MOICA cert extension).
     * This is the primary lookup method for MOICA PKCS#7 login flow.
     *
     * @param cname      the Subject CN from the certificate (person's name)
     * @param last4IDNO  the last 4 digits of the national ID from the certificate extension
     * @param citizenId  the full citizen ID (if available from CN), used as fallback
     * @return the matched or newly created SaUser
     * @throws MoicaUserNotFoundException if autoCreate is disabled and user not found
     */
    @Transactional
    public SaUser syncUserByCnameAndIdno(String cname, String last4IDNO, String citizenId) {
        // Strategy 1: try citizenId lookup first (most precise)
        if (citizenId != null && !citizenId.isBlank()) {
            Optional<SaUser> byCitizenId = saUserRepository.findByCitizenId(citizenId);
            if (byCitizenId.isPresent()) {
                return updateExistingUser(byCitizenId.get(), cname);
            }
        }

        // Strategy 2: try cname + last4IDNO lookup
        if (cname != null && last4IDNO != null) {
            Optional<SaUser> byCnameAndIdno = saUserRepository.findByCnameAndLast4Idno(cname, last4IDNO);
            if (byCnameAndIdno.isPresent()) {
                SaUser user = byCnameAndIdno.get();
                // Bind citizenId if not already set
                if (citizenId != null && user.getCitizenId() == null) {
                    user.setCitizenId(citizenId);
                }
                return updateExistingUser(user, cname);
            }
        }

        // Strategy 3: auto-create new user
        String effectiveId = (citizenId != null && !citizenId.isBlank()) ? citizenId : generateUsername(cname, last4IDNO);
        log.info("Creating new citizen cert user: cname={}, last4IDNO={}, effectiveId={}",
                cname, last4IDNO, effectiveId);
        SaUser newUser = createNewUser(effectiveId, cname);
        if (last4IDNO != null) {
            newUser.setLast4Idno(last4IDNO);
            saUserRepository.save(newUser);
        }
        return newUser;
    }

    private String generateUsername(String cname, String last4IDNO) {
        String base = "CERT_";
        if (cname != null) {
            base += cname.replaceAll("\\s+", "");
        }
        if (last4IDNO != null) {
            base += "_" + last4IDNO;
        }
        return base;
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
