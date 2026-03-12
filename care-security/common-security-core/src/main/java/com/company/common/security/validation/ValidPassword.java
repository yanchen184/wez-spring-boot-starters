package com.company.common.security.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for password complexity requirements.
 * Ensures passwords meet NIST SP 800-63B guidelines:
 *
 * <ul>
 *   <li>Minimum length: 8 characters</li>
 *   <li>At least one uppercase letter (A-Z)</li>
 *   <li>At least one lowercase letter (a-z)</li>
 *   <li>At least one digit (0-9)</li>
 *   <li>At least one special character (!@#$%^&*...)</li>
 * </ul>
 *
 * Usage:
 * <pre>
 * public record CreateUserRequest(
 *     &#64;ValidPassword
 *     String password
 * ) {}
 * </pre>
 *
 * @see PasswordComplexityValidator
 */
@Documented
@Constraint(validatedBy = PasswordComplexityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password must contain at least 8 characters, including uppercase, lowercase, digit, and special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
