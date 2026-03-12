package com.company.common.security.service;

import com.company.common.security.config.PasswordPolicyConfig;
import com.company.common.security.entity.PwdHistory;
import com.company.common.security.password.PasswordPolicyService;
import com.company.common.security.repository.PwdHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing password history and preventing password reuse.
 * Implements HIPAA, PCI-DSS, and NIST SP 800-63B password history requirements.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Checks if new password matches recent passwords</li>
 *   <li>Configurable history count (default: 5)</li>
 *   <li>Works with all password encoding formats (BCrypt, SHA-512, etc.)</li>
 *   <li>Thread-safe</li>
 * </ul>
 *
 * <p>Compliance standards:</p>
 * <ul>
 *   <li>HIPAA: Recommends 4 password history</li>
 *   <li>PCI-DSS: Requires 4 password history</li>
 *   <li>NIST SP 800-63B: Recommends at least 1 password history</li>
 * </ul>
 *
 * @see PasswordPolicyService
 * @see PasswordPolicyConfig
 */
@Service
public class PasswordHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordHistoryService.class);

    private final PwdHistoryRepository pwdHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private PasswordPolicyService policyService;

    public PasswordHistoryService(PwdHistoryRepository pwdHistoryRepository,
                                   PasswordEncoder passwordEncoder) {
        this.pwdHistoryRepository = pwdHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Checks if the new password has been used recently by the user.
     * Returns true if password is reused (should be rejected).
     *
     * @param userId the user ID (must not be null)
     * @param newPassword the plaintext new password (must not be null or empty)
     * @return true if password matches any recent password, false otherwise
     * @throws IllegalArgumentException if userId or newPassword is null/empty
     */
    public boolean isPasswordReused(Long userId, String newPassword) {
        // Validate input parameters
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("newPassword must not be null or empty");
        }

        // Get policy configuration
        PasswordPolicyConfig config;
        if (policyService != null) {
            config = policyService.getGlobalPolicy();
        } else {
            // Fallback for unit tests without Spring context
            config = new PasswordPolicyConfig();
        }

        int historyCount = config.getHistoryCount();

        // If history count is 0, skip history checking
        if (historyCount <= 0) {
            log.debug("Password history checking disabled (historyCount={})", historyCount);
            return false;
        }

        // Fetch recent password hashes
        List<PwdHistory> recentPasswords = pwdHistoryRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, historyCount)
        );

        log.debug("Checking new password against {} recent passwords for user {}", recentPasswords.size(), userId);

        // Check if new password matches any recent password
        for (PwdHistory history : recentPasswords) {
            try {
                if (passwordEncoder.matches(newPassword, history.getPassword())) {
                    log.warn("Password reuse detected for user {}", userId);
                    return true;
                }
            } catch (Exception e) {
                // Log but continue checking - corrupted password hash shouldn't block the whole check
                log.warn("Failed to match password against history entry {} for user {}: {}",
                        history.getObjid(), userId, e.getMessage());
            }
        }

        return false;
    }

    /**
     * Saves the current password to history before changing to a new one.
     * This method should be called BEFORE updating the user's password.
     *
     * @param userId the user ID (must not be null)
     * @param currentPasswordHash the current password hash (not plaintext, must not be null or empty)
     * @throws IllegalArgumentException if userId or currentPasswordHash is null/empty
     */
    public void savePasswordToHistory(Long userId, String currentPasswordHash) {
        // Validate input parameters
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (currentPasswordHash == null || currentPasswordHash.isEmpty()) {
            throw new IllegalArgumentException("currentPasswordHash must not be null or empty");
        }

        try {
            PwdHistory history = new PwdHistory();
            history.setUserId(userId);
            history.setPassword(currentPasswordHash);
            pwdHistoryRepository.save(history);
            log.debug("Saved password to history for user {}", userId);
        } catch (Exception e) {
            // Log but don't fail password change - history is important but not critical
            log.error("Failed to save password to history for user {}: {}", userId, e.getMessage(), e);
        }
    }
}
