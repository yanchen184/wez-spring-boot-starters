package com.company.common.security.controller;

import com.company.common.security.config.PasswordPolicyConfig;
import com.company.common.security.password.PasswordPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Password policy configuration controller for debugging and verification.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /api/v1/password-policy - View current password policy</li>
 *   <li>GET /api/v1/password-policy/providers - View available policy providers</li>
 * </ul>
 *
 * <p>Note: In production, these endpoints should be protected with admin-only access.</p>
 */
@RestController
@RequestMapping("/api/v1/password-policy")
@Tag(name = "Password Policy", description = "Password policy configuration and verification")
public class PasswordPolicyController {

    @Autowired(required = false)
    private PasswordPolicyService policyService;

    /**
     * Gets the currently effective password policy configuration.
     * Useful for debugging and verifying which policy is active.
     *
     * @return current password policy settings
     */
    @GetMapping
    @Operation(summary = "Get current password policy",
               description = "Returns the currently effective password policy configuration")
    public Map<String, Object> getCurrentPolicy() {
        Map<String, Object> response = new HashMap<>();

        if (policyService == null) {
            response.put("error", "PasswordPolicyService not available");
            return response;
        }

        PasswordPolicyConfig config = policyService.getGlobalPolicy();

        response.put("minLength", config.getMinLength());
        response.put("maxLength", config.getMaxLength());
        response.put("requireUppercase", config.isRequireUppercase());
        response.put("requireLowercase", config.isRequireLowercase());
        response.put("requireDigit", config.isRequireDigit());
        response.put("requireSpecialChar", config.isRequireSpecialChar());
        response.put("rejectSequential", config.isRejectSequential());
        response.put("rejectRepeated", config.isRejectRepeated());
        response.put("rejectCommonWeak", config.isRejectCommonWeak());
        response.put("historyCount", config.getHistoryCount());

        // Add example passwords
        response.put("exampleValidPasswords", List.of(
            generateExamplePassword(config),
            "MyS3cur3P@ss!",
            "C0mpl3x#2026"
        ));

        return response;
    }

    /**
     * Lists all available password policy providers in priority order.
     *
     * @return list of provider names
     */
    @GetMapping("/providers")
    @Operation(summary = "List policy providers",
               description = "Returns available password policy providers in priority order")
    public Map<String, Object> getProviders() {
        Map<String, Object> response = new HashMap<>();

        if (policyService == null) {
            response.put("error", "PasswordPolicyService not available");
            return response;
        }

        List<String> providers = policyService.getAvailableProviders();
        response.put("providers", providers);
        response.put("description", "Providers are listed in priority order (highest priority first)");

        return response;
    }

    /**
     * Generates an example valid password based on current policy.
     *
     * @param config password policy configuration
     * @return example password string
     */
    private String generateExamplePassword(PasswordPolicyConfig config) {
        StringBuilder example = new StringBuilder();

        if (config.isRequireUppercase()) {
            example.append("Abc");
        }
        if (config.isRequireLowercase()) {
            example.append("xyz");
        }
        if (config.isRequireDigit()) {
            example.append("123");
        }
        if (config.isRequireSpecialChar()) {
            example.append("!@#");
        }

        // Pad to minimum length if needed
        while (example.length() < config.getMinLength()) {
            example.append("X");
        }

        return example.toString();
    }
}
