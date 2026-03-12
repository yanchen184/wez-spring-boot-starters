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
 * Manages challenge-response for citizen certificate authentication.
 * Generates a random nonce stored in Redis, which the client must sign with
 * their citizen certificate private key to prove possession.
 */
public class CertChallengeService {

    private static final Logger log = LoggerFactory.getLogger(CertChallengeService.class);
    private static final String REDIS_KEY_PREFIX = "cert:challenge:";
    private static final int NONCE_BYTES = 32;

    private final RedisTemplate<String, Object> redisTemplate;
    private final int expireSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public CertChallengeService(RedisTemplate<String, Object> redisTemplate, int expireSeconds) {
        this.redisTemplate = redisTemplate;
        this.expireSeconds = expireSeconds;
    }

    /**
     * Generate a new challenge. The nonce is stored in Redis with a TTL.
     */
    public ChallengeResult generateChallenge() {
        String challengeId = UUID.randomUUID().toString();
        byte[] nonceBytes = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        String redisKey = REDIS_KEY_PREFIX + challengeId;
        redisTemplate.opsForValue().set(redisKey, nonce, Duration.ofSeconds(expireSeconds));

        log.debug("Generated cert challenge: id={}", challengeId);
        return new ChallengeResult(challengeId, nonce);
    }

    /**
     * Consume a challenge (one-time use). Returns the nonce if valid, empty if expired/consumed.
     */
    public Optional<String> consumeChallenge(String challengeId) {
        String redisKey = REDIS_KEY_PREFIX + challengeId;
        Object value = redisTemplate.opsForValue().getAndDelete(redisKey);
        if (value == null) {
            log.debug("Challenge not found or already consumed: id={}", challengeId);
            return Optional.empty();
        }
        return Optional.of(value.toString());
    }

    public record ChallengeResult(String challengeId, String nonce) {}
}
