package com.company.common.security.security;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis-based token blacklist for logout support.
 * When a user logs out, their token's JTI is added to the blacklist
 * with a TTL equal to the token's remaining lifetime.
 */
public class RedisTokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add a token to the blacklist.
     * @param jti the JWT ID (jti claim)
     * @param expiresAt the token's expiration time
     */
    public void blacklist(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isPositive()) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "revoked", ttl);
        }
    }

    /**
     * Check if a token is blacklisted.
     * @param jti the JWT ID (jti claim)
     * @return true if the token has been revoked
     */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
