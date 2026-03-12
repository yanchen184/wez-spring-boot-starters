package com.company.common.security.test;

import com.company.common.security.config.PasswordEncoderConfig;
import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.SaUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@CareSecurityTest
@DisplayName("Phase 3: Password Encoder - Three-Layer Password Mechanism")
class Phase3_PasswordEncoderTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SaUserRepository saUserRepository;

    @Value("${care-test.admin-password}")
    private String adminPassword;

    @Test
    @DisplayName("3.1 BCrypt encode and match")
    void testBcryptEncodeAndMatch() {
        String raw = "TestPassword123!";
        String encoded = passwordEncoder.encode(raw);
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }

    @Test
    @DisplayName("3.2 BCrypt encoded password has {bcrypt}$2b$ prefix")
    void testBcryptPrefixMatch() {
        String encoded = passwordEncoder.encode("SomePassword");
        assertThat(encoded).startsWith("{bcrypt}$2b$12$");
    }

    @Test
    @DisplayName("3.3 Legacy SHA-512 match with embedded salt")
    void testLegacySha512Match() {
        PasswordEncoderConfig.LegacyGrailsPasswordEncoder legacy =
                new PasswordEncoderConfig.LegacyGrailsPasswordEncoder();

        String rawPassword = "password";
        String base64Salt = Base64.getEncoder().encodeToString("testsalt".getBytes(StandardCharsets.UTF_8));
        String input = rawPassword + "{" + base64Salt + "}";
        String hexHash = sha512Hex(input);
        String storedPassword = "{SHA-512}{" + base64Salt + "}" + hexHash;

        assertThat(legacy.matches(rawPassword, storedPassword)).isTrue();
    }

    @Test
    @DisplayName("3.4 Legacy SHA-512 wrong password returns false")
    void testLegacySha512WrongPassword() {
        PasswordEncoderConfig.LegacyGrailsPasswordEncoder legacy =
                new PasswordEncoderConfig.LegacyGrailsPasswordEncoder();

        String base64Salt = Base64.getEncoder().encodeToString("testsalt".getBytes(StandardCharsets.UTF_8));
        String input = "password" + "{" + base64Salt + "}";
        String hexHash = sha512Hex(input);
        String storedPassword = "{SHA-512}{" + base64Salt + "}" + hexHash;

        assertThat(legacy.matches("wrongpassword", storedPassword)).isFalse();
    }

    @Test
    @DisplayName("3.5 Smart matcher detects BCrypt format")
    void testSmartMatcherDetectsBcrypt() {
        String raw = "DetectMe123";
        String bcryptHash = passwordEncoder.encode(raw);
        assertThat(passwordEncoder.matches(raw, bcryptHash)).isTrue();
    }

    @Test
    @DisplayName("3.6 DelegatingPasswordEncoder matches ADMIN password from DB")
    void testVerifyRealAdminPassword() {
        SaUser admin = saUserRepository.findByUsername("ADMIN").orElseThrow();
        String storedHash = admin.getPassword();

        // Both {bcrypt} and {SHA-512} formats should be handled
        assertThat(passwordEncoder.matches(adminPassword, storedHash)).isTrue();
    }

    @Test
    @DisplayName("3.7 Wrong password for ADMIN returns false")
    void testVerifyWrongPasswordForAdmin() {
        SaUser admin = saUserRepository.findByUsername("ADMIN").orElseThrow();
        String storedHash = admin.getPassword();
        assertThat(passwordEncoder.matches("WrongPassword!", storedHash)).isFalse();
    }

    @Test
    @DisplayName("3.8 New password encoding uses BCrypt $2b$ format")
    void testNewPasswordUseBcrypt() {
        String encoded = passwordEncoder.encode("NewPassword123!");
        String rawHash = encoded.replace("{bcrypt}", "");
        assertThat(rawHash).startsWith("$2b$");
    }

    @Test
    @DisplayName("3.9 Legacy encoder returns false for null/non-SHA-512 input")
    void testLegacyNullAndInvalidHandling() {
        PasswordEncoderConfig.LegacyGrailsPasswordEncoder legacy =
                new PasswordEncoderConfig.LegacyGrailsPasswordEncoder();

        assertThat(legacy.matches("password", null)).isFalse();
        assertThat(legacy.matches("password", "plaintext")).isFalse();
        assertThat(legacy.matches("password", "{SHA-512}{nosalt")).isFalse();
    }

    @Test
    @DisplayName("3.10 ADMIN stored password is either {bcrypt} or {SHA-512}")
    void testAdminPasswordFormat() {
        SaUser admin = saUserRepository.findByUsername("ADMIN").orElseThrow();
        String storedHash = admin.getPassword();
        assertThat(storedHash).satisfiesAnyOf(
                hash -> assertThat(hash).startsWith("{bcrypt}"),
                hash -> assertThat(hash).startsWith("{SHA-512}")
        );
    }

    private String sha512Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
