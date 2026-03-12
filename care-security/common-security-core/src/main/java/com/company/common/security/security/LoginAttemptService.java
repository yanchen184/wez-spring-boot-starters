package com.company.common.security.security;

import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.SaUserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages login attempt tracking and account locking.
 */
public class LoginAttemptService {

    private final int maxAttempts;
    private final int lockDurationMinutes;
    private final SaUserRepository saUserRepository;

    public LoginAttemptService(SaUserRepository saUserRepository, int maxAttempts, int lockDurationMinutes) {
        this.saUserRepository = saUserRepository;
        this.maxAttempts = maxAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Transactional
    public void loginSucceeded(String username) {
        saUserRepository.findByUsername(username).ifPresent(user -> {
            user.setLoginFailCount(0);
            user.setLastLoginTime(LocalDateTime.now());
            user.setAccountLocked(false);
            user.setLockTime(null);
            saUserRepository.save(user);
        });
    }

    @Transactional
    public void loginFailed(String username) {
        saUserRepository.findByUsername(username).ifPresent(user -> {
            int failCount = (user.getLoginFailCount() != null ? user.getLoginFailCount() : 0) + 1;
            user.setLoginFailCount(failCount);

            if (failCount >= maxAttempts) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
            }

            saUserRepository.save(user);
        });
    }

    public boolean isLocked(SaUser user) {
        if (!Boolean.TRUE.equals(user.getAccountLocked())) {
            return false;
        }
        // Check if lock duration has passed
        if (user.getLockTime() != null) {
            LocalDateTime unlockTime = user.getLockTime().plusMinutes(lockDurationMinutes);
            if (LocalDateTime.now().isAfter(unlockTime)) {
                // Auto-unlock
                user.setAccountLocked(false);
                user.setLockTime(null);
                user.setLoginFailCount(0);
                saUserRepository.save(user);
                return false;
            }
        }
        return true;
    }
}
