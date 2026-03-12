package com.company.common.security.password;

import com.company.common.security.config.PasswordPolicyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * Central service for retrieving password policy configuration.
 * Automatically selects the highest priority available provider.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Multi-provider support with automatic fallback</li>
 *   <li>Caching to avoid repeated lookups</li>
 *   <li>Logging for debugging policy selection</li>
 *   <li>Thread-safe</li>
 * </ul>
 *
 * <p>Provider priority (lowest @Order value = highest priority):</p>
 * <ol>
 *   <li>Admin UI Provider (@Order(1)) - Real-time admin settings</li>
 *   <li>Database Provider (@Order(50)) - Persistent configuration</li>
 *   <li>YAML Provider (@Order(100)) - Default fallback</li>
 * </ol>
 *
 * <p>Usage in validator:</p>
 * <pre>
 * &#64;Autowired
 * private PasswordPolicyService policyService;
 *
 * PasswordPolicyConfig policy = policyService.getEffectivePolicy(orgId);
 * if (password.length() &lt; policy.getMinLength()) {
 *     // reject
 * }
 * </pre>
 */
public class PasswordPolicyService {

    private static final Logger log = LoggerFactory.getLogger(PasswordPolicyService.class);

    private final List<PasswordPolicyProvider> providers;

    /**
     * Constructor with auto-injected providers (sorted by @Order).
     *
     * @param providers list of providers, Spring automatically sorts by @Order
     */
    public PasswordPolicyService(List<PasswordPolicyProvider> providers) {
        this.providers = providers;
        logAvailableProviders();
    }

    /**
     * Gets the effective password policy for the given organization.
     * Tries providers in priority order until one returns a policy.
     * Result is cached for performance (cache key = orgId).
     *
     * @param orgId organization ID (null for global policy)
     * @return effective password policy configuration
     */
    @Cacheable(value = "passwordPolicies", key = "#orgId")
    public PasswordPolicyConfig getEffectivePolicy(Long orgId) {
        log.debug("Resolving password policy for orgId={}", orgId);

        for (PasswordPolicyProvider provider : providers) {
            if (!provider.isAvailable()) {
                log.debug("Provider [{}] is not available, skipping", provider.getProviderName());
                continue;
            }

            try {
                var policy = provider.getPolicy(orgId);
                if (policy.isPresent()) {
                    log.info("Password policy resolved from provider [{}] for orgId={}",
                            provider.getProviderName(), orgId);
                    return policy.get();
                }
            } catch (Exception e) {
                log.warn("Provider [{}] threw exception, trying next provider: {}",
                        provider.getProviderName(), e.getMessage());
            }
        }

        // Ultimate fallback: hardcoded defaults
        log.warn("No provider returned a policy for orgId={}, using hardcoded defaults", orgId);
        return createDefaultPolicy();
    }

    /**
     * Gets the global password policy (orgId = null).
     */
    public PasswordPolicyConfig getGlobalPolicy() {
        return getEffectivePolicy(null);
    }

    /**
     * Lists all available providers in priority order (for debugging/admin UI).
     */
    public List<String> getAvailableProviders() {
        return providers.stream()
                .filter(PasswordPolicyProvider::isAvailable)
                .map(PasswordPolicyProvider::getProviderName)
                .toList();
    }

    /**
     * Creates the ultimate fallback policy (NIST SP 800-63B defaults).
     */
    private PasswordPolicyConfig createDefaultPolicy() {
        PasswordPolicyConfig config = new PasswordPolicyConfig();
        config.setMinLength(8);
        config.setMaxLength(128);
        config.setRequireUppercase(true);
        config.setRequireLowercase(true);
        config.setRequireDigit(true);
        config.setRequireSpecialChar(true);
        config.setRejectSequential(true);
        config.setRejectRepeated(true);
        config.setRejectCommonWeak(true);
        return config;
    }

    private void logAvailableProviders() {
        log.info("Password policy providers registered (in priority order):");
        for (int i = 0; i < providers.size(); i++) {
            PasswordPolicyProvider provider = providers.get(i);
            log.info("  {}. {} (available: {})",
                    i + 1, provider.getProviderName(), provider.isAvailable());
        }
    }
}
