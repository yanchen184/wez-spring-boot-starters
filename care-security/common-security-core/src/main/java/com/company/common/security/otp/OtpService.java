package com.company.common.security.otp;

import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.SaUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages OTP setup, activation, and verification for users.
 * <p>
 * Flow:
 * 1. User calls POST /api/auth/otp/setup → gets secret + QR URI
 * 2. User scans QR code with authenticator app
 * 3. User calls POST /api/auth/otp/verify-setup with the code → OTP enabled
 * 4. On next login, if OTP enabled, login returns requiresOtp=true
 * 5. User calls POST /api/auth/otp/verify with code → gets JWT tokens
 */
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final TotpService totpService;
    private final SaUserRepository saUserRepository;
    private final String issuer;

    public OtpService(TotpService totpService, SaUserRepository saUserRepository, String issuer) {
        this.totpService = totpService;
        this.saUserRepository = saUserRepository;
        this.issuer = issuer;
    }

    /**
     * Generate a new OTP secret for the user (not yet activated).
     * Stores the secret in DB but keeps otpEnabled=false until verified.
     */
    @Transactional
    public OtpSetupResult setupOtp(String username) {
        SaUser user = saUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String secret = totpService.generateSecret();
        user.setOtpSecret(secret);
        user.setOtpEnabled(false);
        saUserRepository.save(user);

        String otpAuthUri = totpService.buildOtpAuthUri(secret, username, issuer);
        log.info("OTP setup initiated for user: {}", username);
        return new OtpSetupResult(secret, otpAuthUri);
    }

    /**
     * Verify the code from authenticator app and enable OTP for the user.
     */
    @Transactional
    public boolean verifyAndEnableOtp(String username, String code) {
        SaUser user = saUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (user.getOtpSecret() == null) {
            throw new IllegalStateException("OTP not set up for user: " + username);
        }

        if (!totpService.verifyCode(user.getOtpSecret(), code)) {
            return false;
        }

        user.setOtpEnabled(true);
        saUserRepository.save(user);
        log.info("OTP enabled for user: {}", username);
        return true;
    }

    /**
     * Verify OTP code during login.
     */
    public boolean verifyOtp(String username, String code) {
        SaUser user = saUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (user.getOtpSecret() == null || !Boolean.TRUE.equals(user.getOtpEnabled())) {
            return false;
        }

        return totpService.verifyCode(user.getOtpSecret(), code);
    }

    /**
     * Check if OTP is enabled for a user.
     */
    public boolean isOtpEnabled(String username) {
        return saUserRepository.findByUsername(username)
                .map(u -> Boolean.TRUE.equals(u.getOtpEnabled()) && u.getOtpSecret() != null)
                .orElse(false);
    }

    /**
     * Disable OTP for a user (admin operation or user self-service).
     */
    @Transactional
    public void disableOtp(String username) {
        SaUser user = saUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        user.setOtpEnabled(false);
        user.setOtpSecret(null);
        saUserRepository.save(user);
        log.info("OTP disabled for user: {}", username);
    }

    public record OtpSetupResult(String secret, String otpAuthUri) {}
}
