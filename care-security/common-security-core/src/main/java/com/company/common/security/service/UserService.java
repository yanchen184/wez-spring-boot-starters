package com.company.common.security.service;

import com.company.common.response.dto.PageResponse;
import com.company.common.security.dto.request.AssignOrgRoleRequest;
import com.company.common.security.dto.request.CreateUserRequest;
import com.company.common.security.dto.request.UpdateUserRequest;
import com.company.common.security.dto.response.UserResponse;
import com.company.common.security.entity.Organize;
import com.company.common.security.entity.Role;
import com.company.common.security.entity.SaUser;
import com.company.common.security.entity.SaUserOrgRole;
import com.company.common.security.repository.OrganizeRepository;
import com.company.common.security.repository.RoleRepository;
import com.company.common.security.repository.SaUserOrgRoleRepository;
import com.company.common.security.repository.SaUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class UserService {

    private final SaUserRepository saUserRepository;
    private final RoleRepository roleRepository;
    private final OrganizeRepository organizeRepository;
    private final SaUserOrgRoleRepository saUserOrgRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Supplier<? extends SaUser> saUserFactory;

    public UserService(SaUserRepository saUserRepository,
                       RoleRepository roleRepository,
                       OrganizeRepository organizeRepository,
                       SaUserOrgRoleRepository saUserOrgRoleRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       Supplier<? extends SaUser> saUserFactory) {
        this.saUserRepository = saUserRepository;
        this.roleRepository = roleRepository;
        this.organizeRepository = organizeRepository;
        this.saUserOrgRoleRepository = saUserOrgRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.saUserFactory = saUserFactory;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return saUserRepository.findAllWithRoles().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAllByOrg(Long orgId) {
        if (orgId == null) {
            return findAll();
        }
        List<SaUserOrgRole> orgRoles = saUserOrgRoleRepository.findByOrganizeObjid(orgId);
        return orgRoles.stream()
                .map(SaUserOrgRole::getSaUser)
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 分頁搜尋使用者
     *
     * @param keyword     模糊搜尋（username / cname / email），null 或空字串表示不篩選
     * @param orgId       機構 ID，null 表示查全部
     * @param pageRequest 分頁參數（page / size / sortBy / sortDir）
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String keyword, Long orgId,
                                              com.company.common.response.dto.PageRequest pageRequest) {
        String safeSortBy = (pageRequest.getSortBy() != null && SORTABLE_FIELDS.contains(pageRequest.getSortBy()))
                ? pageRequest.getSortBy() : DEFAULT_SORT_FIELD;
        Sort sort = pageRequest.isDescending()
                ? Sort.by(safeSortBy).descending()
                : Sort.by(safeSortBy).ascending();

        Pageable pageable = PageRequest.of(pageRequest.safePage(), pageRequest.safeSize(), sort);

        Page<SaUser> result = (orgId != null)
                ? saUserRepository.searchUsersByOrg(orgId, keyword, pageable)
                : saUserRepository.searchUsers(keyword, pageable);

        List<UserResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private static final String DEFAULT_SORT_FIELD = "username";
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "username", "cname", "email", "enabled", "lastLoginTime", "accountLocked"
    );

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        SaUser user = saUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return toResponse(user);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, String operator) {
        if (saUserRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }

        SaUser user = saUserFactory.get();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCname(request.cname());
        user.setEmail(request.email());
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setAccountExpired(false);
        user.setPasswordExpired(false);
        user.setLoginFailCount(0);
        user.setPasswordChangedDate(LocalDateTime.now());
        user.setCreatedBy(operator);
        user = saUserRepository.save(user);

        // Assign roles
        if (request.roleIds() != null) {
            assignRoles(user, request.roleIds());
        }

        auditService.logEvent("USER_CREATE", operator, request.username(),
                null, null, "User created");

        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, String operator) {
        SaUser user = saUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (request.cname() != null) {
            user.setCname(request.cname());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        user.setLastModifiedBy(operator);

        // Reassign global roles if provided
        if (request.roleIds() != null) {
            saUserOrgRoleRepository.deleteBySaUserObjidAndOrganizeIsNull(id);
            assignRoles(user, request.roleIds());
        }

        saUserRepository.save(user);

        auditService.logEvent("USER_UPDATE", operator, user.getUsername(),
                null, null, "User updated");

        return toResponse(user);
    }

    @Transactional
    public void lockUser(Long id, String operator) {
        SaUser user = saUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now());
        saUserRepository.save(user);

        auditService.logEvent("USER_LOCK", operator, user.getUsername(),
                null, null, "Account locked by admin");
    }

    @Transactional
    public void unlockUser(Long id, String operator) {
        SaUser user = saUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setAccountLocked(false);
        user.setLockTime(null);
        user.setLoginFailCount(0);
        saUserRepository.save(user);

        auditService.logEvent("USER_UNLOCK", operator, user.getUsername(),
                null, null, "Account unlocked by admin");
    }

    @Transactional
    public void resetPassword(Long id, String newPassword, String operator) {
        SaUser user = saUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if ("LDAP".equals(user.getAuthSource())) {
            throw new IllegalArgumentException("Cannot reset password for LDAP user. Manage password in LDAP directory.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordSalt(null);
        user.setPasswordExpired(true); // Force change on next login
        user.setPasswordChangedDate(LocalDateTime.now());
        saUserRepository.save(user);

        auditService.logEvent("PASSWORD_RESET", operator, user.getUsername(),
                null, null, "Password reset by admin");
    }

    @Transactional(readOnly = true)
    public List<UserResponse.OrgRoleInfo> getOrgRoles(Long userId) {
        saUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return saUserOrgRoleRepository.findByUserIdWithRelations(userId).stream()
                .map(uor -> new UserResponse.OrgRoleInfo(
                        uor.getObjid(),
                        uor.getOrganize().getObjid(),
                        uor.getOrganize().getOrgName(),
                        uor.getRole().getObjid(),
                        uor.getRole().getAuthority()))
                .toList();
    }

    @Transactional
    public UserResponse.OrgRoleInfo assignOrgRole(Long userId, AssignOrgRoleRequest request, String operator) {
        SaUser user = saUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.roleId()));

        Organize org = null;
        if (request.orgId() != null) {
            org = organizeRepository.findById(request.orgId())
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + request.orgId()));
        }

        // Duplicate check
        if (request.orgId() != null) {
            saUserOrgRoleRepository.findBySaUserObjidAndRoleObjidAndOrganizeObjid(
                    userId, request.roleId(), request.orgId())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("User already has this role in this organization");
                    });
        } else {
            saUserOrgRoleRepository.findBySaUserObjidAndRoleObjidAndOrganizeIsNull(
                    userId, request.roleId())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("User already has this global role");
                    });
        }

        SaUserOrgRole uor = new SaUserOrgRole();
        uor.setSaUser(user);
        uor.setRole(role);
        uor.setOrganize(org);
        uor = saUserOrgRoleRepository.save(uor);

        String detail = org != null
                ? "Assigned role " + role.getAuthority() + " in org " + org.getOrgName()
                : "Assigned global role " + role.getAuthority();
        auditService.logEvent("ORG_ROLE_ASSIGN", operator, user.getUsername(),
                null, null, detail);

        return new UserResponse.OrgRoleInfo(
                uor.getObjid(),
                org != null ? org.getObjid() : null,
                org != null ? org.getOrgName() : null,
                role.getObjid(), role.getAuthority());
    }

    @Transactional
    public void removeOrgRole(Long userId, Long orgRoleId, String operator) {
        SaUser user = saUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        SaUserOrgRole uor = saUserOrgRoleRepository.findById(orgRoleId)
                .orElseThrow(() -> new IllegalArgumentException("Org role not found: " + orgRoleId));

        // Ownership verification
        if (!uor.getSaUser().getObjid().equals(userId)) {
            throw new IllegalArgumentException("Org role does not belong to this user");
        }

        saUserOrgRoleRepository.delete(uor);

        String detail = uor.getOrganize() != null
                ? "Removed role " + uor.getRole().getAuthority() + " from org " + uor.getOrganize().getOrgName()
                : "Removed global role " + uor.getRole().getAuthority();
        auditService.logEvent("ORG_ROLE_REMOVE", operator, user.getUsername(),
                null, null, detail);
    }

    private void assignRoles(SaUser user, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
            SaUserOrgRole userRole = new SaUserOrgRole();
            userRole.setSaUser(user);
            userRole.setRole(role);
            userRole.setOrganize(null); // global role
            saUserOrgRoleRepository.save(userRole);
        }
    }

    private UserResponse toResponse(SaUser user) {
        List<String> roles = user.getUserRoles().stream()
                .filter(ur -> ur.getOrganize() == null)
                .map(ur -> ur.getRole().getAuthority())
                .toList();

        List<UserResponse.OrgRoleInfo> orgRoles = user.getUserRoles().stream()
                .filter(ur -> ur.getOrganize() != null)
                .map(uor -> new UserResponse.OrgRoleInfo(
                        uor.getObjid(),
                        uor.getOrganize().getObjid(),
                        uor.getOrganize().getOrgName(),
                        uor.getRole().getObjid(),
                        uor.getRole().getAuthority()))
                .toList();

        return new UserResponse(
                user.getObjid(),
                user.getUsername(),
                user.getCname(),
                user.getEmail(),
                user.getEnabled(),
                user.getAccountLocked(),
                user.getLastLoginTime(),
                roles,
                orgRoles,
                user.getAuthSource() != null ? user.getAuthSource() : "LOCAL"
        );
    }
}
