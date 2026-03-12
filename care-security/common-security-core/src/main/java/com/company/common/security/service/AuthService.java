package com.company.common.security.service;

import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.dto.request.ChangePasswordRequest;
import com.company.common.security.dto.request.LoginRequest;
import com.company.common.security.dto.response.MyOrgResponse;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.otp.OtpService;
import com.company.common.security.security.CustomUserDetails;
import com.company.common.security.security.CustomUserDetailsService;
import com.company.common.security.security.LdapAuthenticationProvider;
import com.company.common.security.security.LdapAuthenticationProvider.LdapUserInfo;
import com.company.common.security.security.LoginAttemptService;
import com.company.common.security.security.RedisTokenBlacklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String JWT_ISSUER = "care-security";

    private final int accessTokenTtlMinutes;
    private final int refreshTokenTtlDays;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RedisTokenBlacklistService blacklistService;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;
    private final SaUserRepository saUserRepository;
    private final PasswordHistoryService passwordHistoryService;
    private final LdapAuthenticationProvider ldapAuthProvider;
    private final LdapUserSyncService ldapUserSyncService;
    private final OtpService otpService;
    private final CaptchaService captchaService;

    public AuthService(CustomUserDetailsService userDetailsService,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       JwtDecoder jwtDecoder,
                       RedisTokenBlacklistService blacklistService,
                       LoginAttemptService loginAttemptService,
                       AuditService auditService,
                       SaUserRepository saUserRepository,
                       PasswordHistoryService passwordHistoryService,
                       LdapAuthenticationProvider ldapAuthProvider,
                       LdapUserSyncService ldapUserSyncService,
                       OtpService otpService,
                       CaptchaService captchaService,
                       int accessTokenTtlMinutes,
                       int refreshTokenTtlDays) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.blacklistService = blacklistService;
        this.loginAttemptService = loginAttemptService;
        this.auditService = auditService;
        this.saUserRepository = saUserRepository;
        this.passwordHistoryService = passwordHistoryService;
        this.ldapAuthProvider = ldapAuthProvider;
        this.ldapUserSyncService = ldapUserSyncService;
        this.otpService = otpService;
        this.captchaService = captchaService;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Validate CAPTCHA (if enabled)
        if (captchaService != null) {
            if (request.captchaId() == null || request.captchaAnswer() == null
                    || request.captchaId().isBlank() || request.captchaAnswer().isBlank()) {
                throw new IllegalArgumentException("Captcha is required");
            }
            if (!captchaService.verifyCaptcha(request.captchaId(), request.captchaAnswer())) {
                throw new IllegalArgumentException("Invalid captcha");
            }
        }

        // Try LDAP authentication first (if enabled)
        if (ldapAuthProvider != null) {
            Optional<LdapUserInfo> ldapResult = ldapAuthProvider.authenticate(request.username(), request.password());
            if (ldapResult.isPresent()) {
                return handleLdapLoginSuccess(ldapResult.get(), ipAddress, userAgent);
            }
            // LDAP user not found or bad password -> fall through to local auth
            log.debug("LDAP auth not matched for {}, falling back to local auth", request.username());
        }

        // Local database authentication
        return handleLocalLogin(request, ipAddress, userAgent);
    }

    private TokenResponse handleLdapLoginSuccess(LdapUserInfo ldapUser, String ipAddress, String userAgent) {
        // Sync LDAP user to local DB (create or update)
        SaUser user = ldapUserSyncService.syncUser(ldapUser);

        // Check lock status
        if (loginAttemptService.isLocked(user)) {
            auditService.logLogin(user, ipAddress, userAgent, false, "Account locked");
            throw new LockedException("Account is locked. Please try again later.");
        }

        // Login success
        loginAttemptService.loginSucceeded(ldapUser.username());
        auditService.logLogin(user, ipAddress, userAgent, true, null);
        auditService.logEvent("LOGIN_LDAP", ldapUser.username(), null, ipAddress, userAgent, "LDAP login successful");

        // Check if OTP is required
        if (otpService != null && otpService.isOtpEnabled(ldapUser.username())) {
            log.info("OTP required for LDAP user: {}", ldapUser.username());
            return TokenResponse.otpRequired(ldapUser.username());
        }

        CustomUserDetails userDetails = userDetailsService.loadUserByUsernameAndOrg(ldapUser.username(), null);
        return generateTokenPair(userDetails);
    }

    private TokenResponse handleLocalLogin(LoginRequest request, String ipAddress, String userAgent) {
        SaUser user = saUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // LDAP users should not authenticate locally
        if ("LDAP".equals(user.getAuthSource())) {
            auditService.logLogin(user, ipAddress, userAgent, false, "LDAP user attempted local auth");
            throw new BadCredentialsException("Invalid credentials");
        }

        // Check lock status
        if (loginAttemptService.isLocked(user)) {
            auditService.logLogin(user, ipAddress, userAgent, false, "Account locked");
            throw new LockedException("Account is locked. Please try again later.");
        }

        // Verify password
        boolean matched = verifyPassword(request.password(), user);
        if (!matched) {
            loginAttemptService.loginFailed(request.username());
            auditService.logLogin(user, ipAddress, userAgent, false, "Bad credentials");
            throw new BadCredentialsException("Invalid credentials");
        }

        // Login success
        loginAttemptService.loginSucceeded(request.username());
        auditService.logLogin(user, ipAddress, userAgent, true, null);
        auditService.logEvent("LOGIN", request.username(), null, ipAddress, userAgent, "Login successful");

        // Check if OTP is required
        if (otpService != null && otpService.isOtpEnabled(request.username())) {
            log.info("OTP required for user: {}", request.username());
            return TokenResponse.otpRequired(request.username());
        }

        CustomUserDetails userDetails = userDetailsService.loadUserByUsernameAndOrg(request.username(), null);
        return generateTokenPair(userDetails);
    }

    public TokenResponse refresh(String refreshToken) {
        Jwt jwt = jwtDecoder.decode(refreshToken);

        // Check blacklist
        String jti = jwt.getId();
        if (jti != null && blacklistService.isBlacklisted(jti)) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        // Verify it's a refresh token
        String tokenType = jwt.getClaimAsString("token_type");
        if (!"refresh".equals(tokenType)) {
            throw new BadCredentialsException("Invalid token type");
        }

        // Blacklist old refresh token (rotation)
        if (jti != null) {
            blacklistService.blacklist(jti, jwt.getExpiresAt());
        }

        // Issue new token pair, preserving currentOrgId and impersonatedBy from the old refresh token
        String username = jwt.getSubject();
        Long currentOrgId = jwt.hasClaim("currentOrgId") ? jwt.getClaim("currentOrgId") : null;
        String impersonatedBy = jwt.hasClaim("impersonatedBy") ? jwt.getClaimAsString("impersonatedBy") : null;
        CustomUserDetails userDetails = userDetailsService.loadUserByUsernameAndOrg(username, currentOrgId);
        return generateTokenPair(userDetails, impersonatedBy);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            try {
                Jwt refreshJwt = jwtDecoder.decode(refreshToken);
                if (refreshJwt.getId() != null) {
                    blacklistService.blacklist(refreshJwt.getId(), refreshJwt.getExpiresAt());
                }
            } catch (JwtException ignored) {
                // Token may already be expired or malformed
            }
        }
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        SaUser user = saUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // LDAP users cannot change password here
        if ("LDAP".equals(user.getAuthSource())) {
            throw new BadCredentialsException("LDAP users must change password through the LDAP directory.");
        }

        // Verify current password
        if (!verifyPassword(request.currentPassword(), user)) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Check password history to prevent reuse
        if (passwordHistoryService.isPasswordReused(user.getObjid(), request.newPassword())) {
            throw new BadCredentialsException("New password has been used recently. Please choose a different password.");
        }

        // Save current password to history BEFORE changing
        passwordHistoryService.savePasswordToHistory(user.getObjid(), user.getPassword());

        // Encode new password with BCrypt
        String encodedPassword = passwordEncoder.encode(request.newPassword());

        // Update user password (DelegatingPasswordEncoder already adds {bcrypt} prefix)
        user.setPassword(encodedPassword);
        user.setPasswordSalt(null);
        user.setPasswordChangedDate(LocalDateTime.now());
        user.setPasswordExpired(false);
        saUserRepository.save(user);

        auditService.logEvent("PASSWORD_CHANGE", username, username, null, null, "Password changed");
    }

    /**
     * Verifies if the raw password matches the user's stored password.
     * Delegates to the configured PasswordEncoder which handles:
     * - {bcrypt} prefixed passwords (DelegatingPasswordEncoder)
     * - $2a$/$2b$/$2y$ BCrypt hashes (SmartMatchingEncoder fallback)
     * - {SHA-512}{salt}hash legacy Grails format (SmartMatchingEncoder fallback)
     *
     * @param rawPassword the plaintext password to verify
     * @param user the user whose password to check
     * @return true if password matches, false otherwise
     */
    private boolean verifyPassword(String rawPassword, SaUser user) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Transactional
    public TokenResponse switchUser(String adminUsername, String targetUsername, Long orgId,
                                    String ipAddress, String userAgent) {
        // Verify target user exists
        saUserRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new BadCredentialsException("Target user not found: " + targetUsername));

        // Load target user details with optional org
        CustomUserDetails targetDetails = userDetailsService.loadUserByUsernameAndOrg(targetUsername, orgId);

        // If orgId specified, validate target belongs to that org
        if (orgId != null) {
            boolean belongsToOrg = targetDetails.getOrgRoles().stream()
                    .anyMatch(or -> orgId.equals(or.orgId()));
            if (!belongsToOrg) {
                throw new BadCredentialsException("Target user does not belong to organization: " + orgId);
            }
        }

        // Audit: record impersonation start
        String detail = String.format("Admin [%s] started impersonating user [%s]%s",
                adminUsername, targetUsername,
                orgId != null ? " in org [" + orgId + "]" : "");
        auditService.logEvent("IMPERSONATE_START", adminUsername, targetUsername, ipAddress, userAgent, detail);
        log.warn("IMPERSONATION: {} -> {} (ip={})", adminUsername, targetUsername, ipAddress);

        return generateTokenPair(targetDetails, adminUsername);
    }

    @Transactional
    public TokenResponse exitSwitchUser(String originalAdmin, String impersonatedUser,
                                        String ipAddress, String userAgent) {
        // Audit: record impersonation end
        String detail = String.format("Admin [%s] stopped impersonating user [%s]",
                originalAdmin, impersonatedUser);
        auditService.logEvent("IMPERSONATE_END", originalAdmin, impersonatedUser, ipAddress, userAgent, detail);
        log.info("IMPERSONATION_END: {} returned from {} (ip={})", originalAdmin, impersonatedUser, ipAddress);

        // Reload the original admin user
        CustomUserDetails adminDetails = userDetailsService.loadUserByUsernameAndOrg(originalAdmin, null);
        return generateTokenPair(adminDetails);
    }

    @Transactional(readOnly = true)
    public List<MyOrgResponse> getMyOrgs(String username) {
        CustomUserDetails userDetails = userDetailsService.loadUserByUsernameAndOrg(username, null);
        // Group orgRoles by orgId
        Map<Long, List<CustomUserDetails.OrgRole>> grouped = userDetails.getOrgRoles().stream()
                .filter(or -> or.orgId() != null)
                .collect(Collectors.groupingBy(CustomUserDetails.OrgRole::orgId));

        return grouped.entrySet().stream()
                .map(entry -> new MyOrgResponse(
                        entry.getKey(),
                        entry.getValue().getFirst().orgName(),
                        entry.getValue().stream().map(CustomUserDetails.OrgRole::roleAuthority).toList()))
                .toList();
    }

    @Transactional
    public TokenResponse completeCertLogin(String username, String ipAddress, String userAgent) {
        auditService.logEvent("LOGIN_CERT", username, null, ipAddress, userAgent, "Citizen cert login complete");
        CustomUserDetails userDetails = userDetailsService.loadUserByUsernameAndOrg(username, null);
        return generateTokenPair(userDetails);
    }

    @Transactional
    public TokenResponse completeOtpLogin(String username, String ipAddress, String userAgent) {
        auditService.logEvent("LOGIN_OTP", username, null, ipAddress, userAgent, "OTP verified, login complete");
        CustomUserDetails userDetails = userDetailsService.loadUserByUsernameAndOrg(username, null);
        return generateTokenPair(userDetails);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails getUserInfo(String username, Long currentOrgId) {
        return userDetailsService.loadUserByUsernameAndOrg(username, currentOrgId);
    }

    private TokenResponse generateTokenPair(CustomUserDetails userDetails) {
        return generateTokenPair(userDetails, null);
    }

    private TokenResponse generateTokenPair(CustomUserDetails userDetails, String impersonatedBy) {
        Instant now = Instant.now();
        Long currentOrgId = userDetails.getCurrentOrgId();

        // Access token
        String accessJti = UUID.randomUUID().toString();
        JwtClaimsSet.Builder accessClaimsBuilder = JwtClaimsSet.builder()
                .subject(userDetails.getUsername())
                .issuer(JWT_ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES))
                .id(accessJti)
                .claim("token_type", "access")
                .claim("userId", userDetails.getUserId())
                .claim("cname", userDetails.getCname())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .claim("authSource", userDetails.getAuthSource());

        if (currentOrgId != null) {
            accessClaimsBuilder.claim("currentOrgId", currentOrgId);
        }
        if (impersonatedBy != null) {
            accessClaimsBuilder.claim("impersonatedBy", impersonatedBy);
        }

        JwtClaimsSet accessClaims = accessClaimsBuilder.build();

        JwtEncoderParameters accessParams = JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), accessClaims);
        String accessToken = jwtEncoder.encode(accessParams).getTokenValue();

        // Refresh token (minimal claims + currentOrgId for persistence across refresh)
        String refreshJti = UUID.randomUUID().toString();
        JwtClaimsSet.Builder refreshClaimsBuilder = JwtClaimsSet.builder()
                .subject(userDetails.getUsername())
                .issuer(JWT_ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .id(refreshJti)
                .claim("token_type", "refresh");

        if (currentOrgId != null) {
            refreshClaimsBuilder.claim("currentOrgId", currentOrgId);
        }
        if (impersonatedBy != null) {
            refreshClaimsBuilder.claim("impersonatedBy", impersonatedBy);
        }

        JwtClaimsSet refreshClaims = refreshClaimsBuilder.build();

        JwtEncoderParameters refreshParams = JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), refreshClaims);
        String refreshTokenValue = jwtEncoder.encode(refreshParams).getTokenValue();

        return new TokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                accessTokenTtlMinutes * 60L,
                userDetails.getUsername(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList(),
                currentOrgId,
                impersonatedBy,
                userDetails.getAuthSource()
        );
    }

}
