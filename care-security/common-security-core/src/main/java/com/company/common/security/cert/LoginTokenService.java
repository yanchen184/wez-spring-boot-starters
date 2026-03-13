package com.company.common.security.cert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages login tokens for MOICA citizen certificate authentication.
 * The server generates a random login token; the client signs it via PKCS#7 to prove possession
 * of the citizen certificate private key. Each token is single-use (stored in Redis with TTL).
 */
public class LoginTokenService {

    private static final Logger log = LoggerFactory.getLogger(LoginTokenService.class);
    private static final String REDIS_KEY_PREFIX = "cert:login-token:";
    private static final int TOKEN_BYTES = 32;

    private final RedisTemplate<String, Object> redisTemplate;
    private final int expireSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginTokenService(RedisTemplate<String, Object> redisTemplate, int expireSeconds) {
        this.redisTemplate = redisTemplate;
        this.expireSeconds = expireSeconds;
    }

    /**
     * Generate a new login token. The token value is stored in Redis with a TTL.
     * The client must sign this token value using their citizen certificate.
     *
     * @return the generated login token string (URL-safe Base64)
     */
    public String generateLoginToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String loginToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        String redisKey = REDIS_KEY_PREFIX + loginToken;
        // Store "1" as marker; the token itself is the key
        redisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(expireSeconds));

        log.debug("Generated login token (length={})", loginToken.length());
        return loginToken;
    }

    /**
     * Consume a login token (one-time use). Returns true if the token was valid and not yet consumed.
     * The token is deleted from Redis after consumption to prevent replay attacks.
     *
     * @param loginToken the token to consume
     * @return true if valid and consumed, false if expired or already consumed
     */
    public boolean consumeLoginToken(String loginToken) {
        if (loginToken == null || loginToken.isBlank()) {
            return false;
        }
        String redisKey = REDIS_KEY_PREFIX + loginToken;
        Object value = redisTemplate.opsForValue().getAndDelete(redisKey);
        if (value == null) {
            log.debug("Login token not found or already consumed");
            return false;
        }
        log.debug("Login token consumed successfully");
        return true;
    }
}
