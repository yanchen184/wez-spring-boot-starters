package com.company.common.security.otp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * TOTP (Time-based One-Time Password) implementation per RFC 6238.
 * <p>
 * Generates and verifies 6-digit codes compatible with
 * Google Authenticator / Microsoft Authenticator.
 */
public class TotpService {

    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int SECRET_BYTE_LENGTH = 20;
    private static final int[] DIGITS_POWER = {1, 10, 100, 1000, 10000, 100000, 1000000};

    private final int allowedSkew;

    /**
     * @param allowedSkew number of time steps to allow before/after current time (1 = +/- 30 seconds)
     */
    public TotpService(int allowedSkew) {
        this.allowedSkew = allowedSkew;
    }

    /**
     * Generate a random Base32-encoded secret key for a new user.
     */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTE_LENGTH];
        new SecureRandom().nextBytes(bytes);
        return Base32.encode(bytes);
    }

    /**
     * Generate the current TOTP code for the given secret.
     */
    public String generateCode(String base32Secret) {
        return generateCode(base32Secret, Instant.now());
    }

    public String generateCode(String base32Secret, Instant time) {
        long timeStep = time.getEpochSecond() / TIME_STEP_SECONDS;
        byte[] key = Base32.decode(base32Secret);
        return computeTotp(key, timeStep);
    }

    /**
     * Verify a TOTP code against the given secret, allowing for clock skew.
     */
    public boolean verifyCode(String base32Secret, String code) {
        return verifyCode(base32Secret, code, Instant.now());
    }

    public boolean verifyCode(String base32Secret, String code, Instant time) {
        long timeStep = time.getEpochSecond() / TIME_STEP_SECONDS;
        byte[] key = Base32.decode(base32Secret);

        for (int i = -allowedSkew; i <= allowedSkew; i++) {
            String computed = computeTotp(key, timeStep + i);
            if (constantTimeEquals(computed, code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build an otpauth:// URI for QR code generation.
     * Compatible with Google Authenticator / Microsoft Authenticator.
     */
    public String buildOtpAuthUri(String secret, String username, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                urlEncode(issuer), urlEncode(username), secret, urlEncode(issuer),
                CODE_DIGITS, TIME_STEP_SECONDS);
    }

    private String computeTotp(byte[] key, long timeStep) {
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(timeBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % DIGITS_POWER[CODE_DIGITS];
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute TOTP", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Minimal Base32 encoder/decoder (RFC 4648) for TOTP secrets.
     */
    public static final class Base32 {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        public static String encode(byte[] data) {
            StringBuilder sb = new StringBuilder();
            int buffer = 0, bitsLeft = 0;
            for (byte b : data) {
                buffer = (buffer << 8) | (b & 0xFF);
                bitsLeft += 8;
                while (bitsLeft >= 5) {
                    bitsLeft -= 5;
                    sb.append(ALPHABET.charAt((buffer >> bitsLeft) & 0x1F));
                }
            }
            if (bitsLeft > 0) {
                sb.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
            }
            return sb.toString();
        }

        public static byte[] decode(String base32) {
            String upper = base32.toUpperCase().replaceAll("[= ]", "");
            byte[] result = new byte[upper.length() * 5 / 8];
            int buffer = 0, bitsLeft = 0, index = 0;
            for (char c : upper.toCharArray()) {
                int val = ALPHABET.indexOf(c);
                if (val < 0) throw new IllegalArgumentException("Invalid Base32 character: " + c);
                buffer = (buffer << 5) | val;
                bitsLeft += 5;
                if (bitsLeft >= 8) {
                    bitsLeft -= 8;
                    result[index++] = (byte) (buffer >> bitsLeft);
                }
            }
            return result;
        }

        private Base32() {}
    }
}
