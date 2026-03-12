package com.company.common.security.password;

import com.company.common.security.config.PasswordPolicyConfig;

import java.util.Optional;

/**
 * Strategy interface for providing password policy configuration.
 * Implementations can load policy from various sources:
 * <ul>
 *   <li>YAML configuration files (default)</li>
 *   <li>Database tables (future)</li>
 *   <li>Remote configuration services (future)</li>
 *   <li>Admin UI settings (future)</li>
 * </ul>
 *
 * <p>Multiple providers can coexist. Use {@code @Order} to control priority:</p>
 * <pre>
 * &#64;Component
 * &#64;Order(1)  // Highest priority
 * public class AdminUiPolicyProvider implements PasswordPolicyProvider { ... }
 *
 * &#64;Component
 * &#64;Order(50)
 * public class DatabasePolicyProvider implements PasswordPolicyProvider { ... }
 *
 * &#64;Component
 * &#64;Order(100)  // Lowest priority (fallback)
 * public class YamlPolicyProvider implements PasswordPolicyProvider { ... }
 * </pre>
 *
 * @see PasswordPolicyService
 * @see PasswordPolicyConfig
 */
public interface PasswordPolicyProvider {

    /**
     * Returns the password policy configuration.
     * Return {@code Optional.empty()} if this provider cannot provide a policy
     * (e.g., database not configured, admin settings not available).
     *
     * @param orgId organization ID for multi-tenant support, null for global policy
     * @return password policy configuration, or empty if not available
     */
    Optional<PasswordPolicyConfig> getPolicy(Long orgId);

    /**
     * Returns the name of this provider for logging/debugging.
     * Examples: "YAML", "Database", "AdminUI", "Remote"
     */
    String getProviderName();

    /**
     * Checks if this provider is currently available/enabled.
     * Return false if the provider's dependencies are not ready
     * (e.g., database not connected, remote service down).
     *
     * @return true if this provider can provide policies
     */
    default boolean isAvailable() {
        return true;
    }
}
