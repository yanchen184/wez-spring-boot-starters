package com.company.common.hub.service;

import com.company.common.hub.exception.HubTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT Token 簽發、驗證、黑名單服務。
 *
 * <p>使用 HS256 演算法，Claims 包含 userId / orgId / hubSetId。
 * 黑名單以 ConcurrentHashMap 實作（日後可改為 Redis）。
 */
public class HubTokenService {

    private static final Logger log = LoggerFactory.getLogger(HubTokenService.class);

    /** Token 解密失敗。 */
    public static final String CODE_INVALID = "422001";

    /** Token 已過期。 */
    public static final String CODE_EXPIRED = "422002";

    private final SecretKey secretKey;
    private final Map<String, Boolean> blacklist = new ConcurrentHashMap<>();

    public HubTokenService(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 簽發 JWT Token。
     *
     * @param userId        使用者 ID
     * @param orgId         單位 ID（nullable）
     * @param hubSetId      API 設定 ID
     * @param agingSeconds  有效秒數
     * @return JWT Token 字串
     */
    public String issueToken(Long userId, Long orgId, Long hubSetId, int agingSeconds) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(agingSeconds);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("hubSetId", hubSetId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey);

        if (orgId != null) {
            builder.claim("orgId", orgId);
        }

        return builder.compact();
    }

    /**
     * 驗證 JWT Token 並解析 Claims。
     *
     * @param token JWT Token 字串
     * @return Claims
     * @throws HubTokenException 422002 Token 已過期，422001 Token 無效
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new HubTokenException(CODE_EXPIRED, "Token 已過期", e);
        } catch (JwtException e) {
            throw new HubTokenException(CODE_INVALID, "Token 解密失敗", e);
        }
    }

    /**
     * 將 Token 加入黑名單。
     *
     * @param token JWT Token 字串
     */
    public void blacklistToken(String token) {
        blacklist.put(token, Boolean.TRUE);
        log.info("Token 已加入黑名單");
    }

    /**
     * 檢查 Token 是否在黑名單中。
     *
     * @param token JWT Token 字串
     * @return true 表示已被列入黑名單
     */
    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }
}
