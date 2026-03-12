package com.company.common.jpa.auditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * 預設審計人提供者
 *
 * <p>如果 classpath 有 Spring Security，從 SecurityContext 取得當前使用者名稱。
 * 若無 Spring Security 或無認證資訊（如排程任務、系統初始化），回傳 "SYSTEM"。
 *
 * <p>消費端可自行定義 {@code AuditorAware<String>} Bean 來覆蓋此預設行為。
 */
public class DefaultAuditorAware implements AuditorAware<String> {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuditorAware.class);
    private static final boolean SECURITY_PRESENT = isSecurityPresent();

    private static boolean isSecurityPresent() {
        try {
            Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        if (SECURITY_PRESENT) {
            try {
                return SecurityAuditorHelper.getAuditor();
            } catch (Exception e) {
                log.warn("Failed to get current auditor from SecurityContext, falling back to SYSTEM", e);
            }
        }
        return Optional.of("SYSTEM");
    }

    /**
     * 獨立內部類，避免沒有 Spring Security 時觸發 NoClassDefFoundError
     */
    private static class SecurityAuditorHelper {
        static Optional<String> getAuditor() {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of(authentication.getName());
            }
            return Optional.empty();
        }
    }
}
