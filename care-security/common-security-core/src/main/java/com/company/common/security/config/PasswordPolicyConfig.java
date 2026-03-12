package com.company.common.security.config;

/**
 * Password policy configuration for complexity validation and history checking.
 * Can be customized via application properties under {@code care.security.password}.
 *
 * <p>Example configuration in application.yml:</p>
 * <pre>
 * care:
 *   security:
 *     password:
 *       min-length: 8
 *       max-length: 128
 *       require-uppercase: true
 *       require-lowercase: true
 *       require-digit: true
 *       require-special-char: true
 *       reject-sequential: true
 *       reject-repeated: true
 *       reject-common-weak: true
 *       history-count: 5              # Prevent reuse of last 5 passwords
 * </pre>
 *
 * <p>Compliance standards:</p>
 * <ul>
 *   <li>NIST SP 800-63B: Recommends 8+ characters, special chars, no sequential patterns</li>
 *   <li>HIPAA: Recommends 10+ characters, 4 password history</li>
 *   <li>PCI-DSS: Requires 12+ characters, 4 password history</li>
 * </ul>
 */
public class PasswordPolicyConfig {

    /**
     * Minimum password length (default: 8).
     */
    private int minLength = 8;

    /**
     * Maximum password length to prevent DoS (default: 128).
     */
    private int maxLength = 128;

    /**
     * Require at least one uppercase letter A-Z (default: true).
     */
    private boolean requireUppercase = true;

    /**
     * Require at least one lowercase letter a-z (default: true).
     */
    private boolean requireLowercase = true;

    /**
     * Require at least one digit 0-9 (default: true).
     */
    private boolean requireDigit = true;

    /**
     * Require at least one special character (default: true).
     * Special characters: !@#$%^&*()_+-=[]{}|;:',.<>?/~`
     */
    private boolean requireSpecialChar = true;

    /**
     * Reject passwords with sequential characters like "abc", "123" (default: true).
     */
    private boolean rejectSequential = true;

    /**
     * Reject passwords with 3+ repeated characters like "aaa", "111" (default: true).
     */
    private boolean rejectRepeated = true;

    /**
     * Reject passwords containing common weak patterns like "password", "qwerty" (default: true).
     */
    private boolean rejectCommonWeak = true;

    /**
     * Number of recent passwords to check for reuse prevention (default: 5).
     * Set to 0 to disable password history checking.
     * HIPAA recommends 4, PCI-DSS recommends 4, NIST recommends at least 1.
     */
    private int historyCount = 5;

    // Getters and Setters
    public int getMinLength() { return minLength; }
    public void setMinLength(int minLength) { this.minLength = minLength; }

    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }

    public boolean isRequireUppercase() { return requireUppercase; }
    public void setRequireUppercase(boolean requireUppercase) { this.requireUppercase = requireUppercase; }

    public boolean isRequireLowercase() { return requireLowercase; }
    public void setRequireLowercase(boolean requireLowercase) { this.requireLowercase = requireLowercase; }

    public boolean isRequireDigit() { return requireDigit; }
    public void setRequireDigit(boolean requireDigit) { this.requireDigit = requireDigit; }

    public boolean isRequireSpecialChar() { return requireSpecialChar; }
    public void setRequireSpecialChar(boolean requireSpecialChar) { this.requireSpecialChar = requireSpecialChar; }

    public boolean isRejectSequential() { return rejectSequential; }
    public void setRejectSequential(boolean rejectSequential) { this.rejectSequential = rejectSequential; }

    public boolean isRejectRepeated() { return rejectRepeated; }
    public void setRejectRepeated(boolean rejectRepeated) { this.rejectRepeated = rejectRepeated; }

    public boolean isRejectCommonWeak() { return rejectCommonWeak; }
    public void setRejectCommonWeak(boolean rejectCommonWeak) { this.rejectCommonWeak = rejectCommonWeak; }

    public int getHistoryCount() { return historyCount; }
    public void setHistoryCount(int historyCount) { this.historyCount = historyCount; }
}
