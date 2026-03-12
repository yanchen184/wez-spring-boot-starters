package com.company.common.security.validation;

import com.company.common.security.config.PasswordPolicyConfig;
import com.company.common.security.password.PasswordPolicyService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Pattern;

/**
 * Validator implementation for {@link ValidPassword} annotation.
 * Validates password complexity according to dynamically resolved policy.
 *
 * <p>Policy resolution order (highest priority first):</p>
 * <ol>
 *   <li>Admin UI settings (if configured)</li>
 *   <li>Database configuration (if available)</li>
 *   <li>YAML application properties (default fallback)</li>
 * </ol>
 *
 * <p>Default requirements (NIST SP 800-63B):</p>
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least 1 uppercase letter (A-Z)</li>
 *   <li>At least 1 lowercase letter (a-z)</li>
 *   <li>At least 1 digit (0-9)</li>
 *   <li>At least 1 special character (!@#$%^&*()_+-=[]{}|;:',.<>?/~`)</li>
 * </ul>
 *
 * <p>Optional rejections (enabled by default):</p>
 * <ul>
 *   <li>Sequential characters (abc, 123)</li>
 *   <li>Repeated characters (aaa, 111)</li>
 *   <li>Common patterns (qwerty, password)</li>
 * </ul>
 *
 * @see PasswordPolicyService
 * @see PasswordPolicyConfig
 */
public class PasswordComplexityValidator implements ConstraintValidator<ValidPassword, String> {

    @Autowired(required = false)
    private PasswordPolicyService policyService;

    // Regex patterns for password complexity (compiled once for performance)
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]");

    // Common weak patterns to reject (case-insensitive)
    private static final Pattern SEQUENTIAL_PATTERN = Pattern.compile(
            "(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz|" +
            "012|123|234|345|456|567|678|789)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REPEATED_PATTERN = Pattern.compile("(.)\\1{2,}"); // 3+ repeated chars
    private static final int MIN_REPEATED_CHARS = 3;
    private static final String[] COMMON_WEAK_PASSWORDS = {
            "password", "passw0rd", "admin", "administrator", "root", "user",
            "qwerty", "123456", "welcome", "letmein", "monkey", "dragon"
    };

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null or empty passwords are handled by @NotBlank
        if (password == null || password.isEmpty()) {
            return true;
        }

        // Resolve policy from service (auto-selects highest priority provider)
        // Falls back to default if service not available (e.g., unit tests)
        PasswordPolicyConfig config;
        if (policyService != null) {
            config = policyService.getGlobalPolicy();
        } else {
            // Fallback for unit tests without Spring context
            config = new PasswordPolicyConfig();
        }

        // Check minimum length
        if (password.length() < config.getMinLength()) {
            setCustomMessage(context, "Password must be at least " + config.getMinLength() + " characters long");
            return false;
        }

        // Check maximum length (prevent DoS)
        if (password.length() > config.getMaxLength()) {
            setCustomMessage(context, "Password must not exceed " + config.getMaxLength() + " characters");
            return false;
        }

        // Check for uppercase letter
        if (config.isRequireUppercase() && !UPPERCASE_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must contain at least one uppercase letter (A-Z)");
            return false;
        }

        // Check for lowercase letter
        if (config.isRequireLowercase() && !LOWERCASE_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must contain at least one lowercase letter (a-z)");
            return false;
        }

        // Check for digit
        if (config.isRequireDigit() && !DIGIT_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must contain at least one digit (0-9)");
            return false;
        }

        // Check for special character
        if (config.isRequireSpecialChar() && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must contain at least one special character (!@#$%^&*...)");
            return false;
        }

        // Check for sequential characters
        if (config.isRejectSequential() && SEQUENTIAL_PATTERN.matcher(password.toLowerCase()).find()) {
            setCustomMessage(context, "Password must not contain sequential characters (e.g., abc, 123)");
            return false;
        }

        // Check for repeated characters
        if (config.isRejectRepeated() && REPEATED_PATTERN.matcher(password).find()) {
            setCustomMessage(context, "Password must not contain 3 or more repeated characters");
            return false;
        }

        // Check against common weak passwords
        if (config.isRejectCommonWeak()) {
            String lowerPassword = password.toLowerCase();
            for (String weakPassword : COMMON_WEAK_PASSWORDS) {
                if (lowerPassword.contains(weakPassword)) {
                    setCustomMessage(context, "Password contains common weak patterns and is not allowed");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Sets a custom validation message.
     */
    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
