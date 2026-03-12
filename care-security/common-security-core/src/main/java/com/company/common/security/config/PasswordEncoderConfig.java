package com.company.common.security.config;

import com.password4j.BcryptFunction;
import com.password4j.Password;
import com.password4j.types.Bcrypt;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class PasswordEncoderConfig {

    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new Password4jBcryptEncoder());
        encoders.put("legacy", new LegacyGrailsPasswordEncoder());

        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("bcrypt", encoders);
        delegating.setDefaultPasswordEncoderForMatches(new SmartMatchingEncoder());
        return delegating;
    }

    /**
     * Password4j BCrypt encoder - used for new passwords.
     */
    static class Password4jBcryptEncoder implements PasswordEncoder {

        private static final BcryptFunction BCRYPT = BcryptFunction.getInstance(Bcrypt.B, 12);

        @Override
        public String encode(CharSequence rawPassword) {
            String hash = Password.hash(rawPassword.toString())
                    .with(BCRYPT)
                    .getResult();
            if (hash == null) {
                throw new IllegalStateException("BCrypt hash returned null");
            }
            return hash;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return Password.check(rawPassword.toString(), encodedPassword != null ? encodedPassword : "")
                    .with(BCRYPT);
        }
    }

    /**
     * Legacy Grails password encoder for backward compatibility.
     * Handles the Grails Spring Security format: {SHA-512}{base64salt}hexhash
     * Algorithm: SHA-512(rawPassword + "{" + base64salt + "}")
     */
    public static class LegacyGrailsPasswordEncoder implements PasswordEncoder {

        private static final String SHA512_PREFIX = "{SHA-512}";

        @Override
        public String encode(CharSequence rawPassword) {
            throw new UnsupportedOperationException("Legacy encoder is read-only for migration");
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            if (encodedPassword == null || !encodedPassword.startsWith(SHA512_PREFIX)) {
                return false;
            }
            return matchesSha512(rawPassword.toString(), encodedPassword);
        }

        /**
         * Parse {SHA-512}{base64salt}hexhash and verify.
         */
        private boolean matchesSha512(String rawPassword, String storedPassword) {
            try {
                String afterPrefix = storedPassword.substring(SHA512_PREFIX.length());
                // afterPrefix = {base64salt}hexhash
                int closeBrace = afterPrefix.indexOf('}');
                if (closeBrace < 1) return false;

                String base64Salt = afterPrefix.substring(1, closeBrace);
                String hexHash = afterPrefix.substring(closeBrace + 1);

                // Grails format: SHA-512(password + "{" + salt + "}")
                String input = rawPassword + "{" + base64Salt + "}";
                MessageDigest md = MessageDigest.getInstance("SHA-512");
                byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
                String computed = bytesToHex(digest);
                return computed.equalsIgnoreCase(hexHash);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-512 not available", e);
            }
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    /**
     * Smart encoder that auto-detects password format:
     * - BCrypt: $2b$, $2a$, $2y$ prefix
     * - Legacy Grails: {SHA-512}{salt}hash prefix
     */
    static class SmartMatchingEncoder implements PasswordEncoder {

        private final Password4jBcryptEncoder bcryptEncoder = new Password4jBcryptEncoder();
        private final LegacyGrailsPasswordEncoder legacyEncoder = new LegacyGrailsPasswordEncoder();

        @Override
        public String encode(CharSequence rawPassword) {
            return bcryptEncoder.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            if (encodedPassword == null) return false;

            if (encodedPassword.startsWith("$2b$") ||
                    encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2y$")) {
                return bcryptEncoder.matches(rawPassword, encodedPassword);
            }

            if (encodedPassword.startsWith("{SHA-512}")) {
                return legacyEncoder.matches(rawPassword, encodedPassword);
            }

            return false;
        }
    }
}
