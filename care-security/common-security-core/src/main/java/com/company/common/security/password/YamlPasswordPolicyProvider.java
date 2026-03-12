package com.company.common.security.password;

import com.company.common.security.config.PasswordPolicyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import java.util.Optional;

/**
 * Password policy provider that loads configuration from application.yml.
 * This is the default fallback provider with lowest priority.
 *
 * <p>Configuration example:</p>
 * <pre>
 * care:
 *   security:
 *     password:
 *       min-length: 8
 *       require-uppercase: true
 *       require-lowercase: true
 *       # ... other settings
 * </pre>
 *
 * <p>Priority: {@code @Order(100)} (lowest, used as fallback)</p>
 *
 * @see PasswordPolicyProvider
 * @see PasswordPolicyService
 */
@Order(100)  // Lowest priority (fallback)
public class YamlPasswordPolicyProvider implements PasswordPolicyProvider {

    private static final Logger log = LoggerFactory.getLogger(YamlPasswordPolicyProvider.class);

    private final PasswordPolicyConfig yamlConfig;

    /**
     * Constructor with YAML configuration injected from CareSecurityProperties.
     *
     * @param yamlConfig password policy from application.yml
     */
    public YamlPasswordPolicyProvider(PasswordPolicyConfig yamlConfig) {
        this.yamlConfig = yamlConfig;
        log.info("YamlPasswordPolicyProvider initialized with config: minLength={}, requireUppercase={}, requireLowercase={}, requireDigit={}, requireSpecialChar={}",
                yamlConfig.getMinLength(),
                yamlConfig.isRequireUppercase(),
                yamlConfig.isRequireLowercase(),
                yamlConfig.isRequireDigit(),
                yamlConfig.isRequireSpecialChar());
    }

    @Override
    public Optional<PasswordPolicyConfig> getPolicy(Long orgId) {
        // YAML configuration is global, ignore orgId
        log.debug("Returning YAML-based password policy (orgId={} ignored, YAML is global)", orgId);
        return Optional.of(yamlConfig);
    }

    @Override
    public String getProviderName() {
        return "YAML";
    }

    @Override
    public boolean isAvailable() {
        return yamlConfig != null;
    }
}
