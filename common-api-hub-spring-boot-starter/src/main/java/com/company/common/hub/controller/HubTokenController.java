package com.company.common.hub.controller;

import com.company.common.hub.dto.HubAuthResult;
import com.company.common.hub.dto.HubResponse;
import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.dto.TokenRequest;
import com.company.common.hub.dto.TokenResponse;
import com.company.common.hub.exception.HubAuthException;
import com.company.common.hub.exception.HubTokenException;
import com.company.common.hub.service.HubAuthService;
import com.company.common.hub.service.HubLogService;
import com.company.common.hub.service.HubTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hub Token 端點。
 *
 * <ul>
 *   <li>{@code POST /api/hub/token} — 帳密換 Token</li>
 *   <li>{@code POST /api/hub/token/refresh} — Token 續期</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/hub/token")
public class HubTokenController {

    private static final Logger log = LoggerFactory.getLogger(HubTokenController.class);

    private final HubAuthService hubAuthService;
    private final HubTokenService hubTokenService;
    private final HubLogService hubLogService;

    public HubTokenController(HubAuthService hubAuthService,
                               HubTokenService hubTokenService,
                               HubLogService hubLogService) {
        this.hubAuthService = hubAuthService;
        this.hubTokenService = hubTokenService;
        this.hubLogService = hubLogService;
    }

    /**
     * 帳密換 Token。
     *
     * @param tokenRequest 包含 username / password / uri
     * @param request      HTTP 請求（取 IP）
     * @return Token 回應
     */
    @PostMapping
    public ResponseEntity<HubResponse<?>> issueToken(
            @RequestBody TokenRequest tokenRequest,
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String clientIp = request.getRemoteAddr();

        try {
            HubAuthResult authResult = hubAuthService.authenticate(
                    tokenRequest.getUri(), "POST",
                    tokenRequest.getUsername(), tokenRequest.getPassword(),
                    null, clientIp
            );

            int aging = authResult.getHubSet().getJwtTokenAging();
            String token = hubTokenService.issueToken(
                    authResult.getHubUser().getId(),
                    authResult.getHubUser().getOrgId(),
                    authResult.getHubSet().getId(),
                    aging
            );

            long elapsed = System.currentTimeMillis() - startTime;
            hubLogService.log(authResult.getHubUserSet(), "POST", "/api/hub/token",
                    null, clientIp, true, HubResponseCode.TOKEN_ISSUED, null, null, elapsed);

            TokenResponse tokenResponse = new TokenResponse(token, aging);
            return ResponseEntity.ok(
                    HubResponse.success(HubResponseCode.TOKEN_ISSUED, "Token 簽發成功", tokenResponse)
            );

        } catch (HubAuthException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            hubLogService.log(null, "POST", "/api/hub/token",
                    null, clientIp, false, ex.getCode(), null, ex.getMessage(), elapsed);
            return ResponseEntity.status(401).body(
                    HubResponse.error(ex.getCode(), ex.getMessage())
            );
        }
    }

    /**
     * Token 續期。
     *
     * @param token   舊 Token（從 X-Hub-Token header 取得）
     * @param request HTTP 請求
     * @return 新 Token 回應
     */
    @PostMapping("/refresh")
    public ResponseEntity<HubResponse<?>> refreshToken(
            @RequestHeader("X-Hub-Token") String token,
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String clientIp = request.getRemoteAddr();

        try {
            Claims claims = hubTokenService.validateToken(token);
            if (hubTokenService.isBlacklisted(token)) {
                throw new HubAuthException(HubResponseCode.TOKEN_INVALID, "Token 已被列入黑名單");
            }

            Long userId = claims.get("userId", Long.class);
            Long orgId = claims.get("orgId", Long.class);
            Long hubSetId = claims.get("hubSetId", Long.class);

            // 預設續期 3600 秒（可從 HubSet 查詢，但這裡簡化）
            int aging = 3600;
            String newToken = hubTokenService.issueToken(userId, orgId, hubSetId, aging);

            // 舊 Token 加入黑名單
            hubTokenService.blacklistToken(token);

            long elapsed = System.currentTimeMillis() - startTime;
            hubLogService.log(null, "POST", "/api/hub/token/refresh",
                    null, clientIp, true, HubResponseCode.TOKEN_REFRESHED, null, null, elapsed);

            TokenResponse tokenResponse = new TokenResponse(newToken, aging);
            return ResponseEntity.ok(
                    HubResponse.success(HubResponseCode.TOKEN_REFRESHED, "Token 續期成功", tokenResponse)
            );

        } catch (HubTokenException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            hubLogService.log(null, "POST", "/api/hub/token/refresh",
                    null, clientIp, false, ex.getCode(), null, ex.getMessage(), elapsed);
            return ResponseEntity.status(422).body(
                    HubResponse.error(ex.getCode(), ex.getMessage())
            );
        } catch (HubAuthException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            hubLogService.log(null, "POST", "/api/hub/token/refresh",
                    null, clientIp, false, ex.getCode(), null, ex.getMessage(), elapsed);
            return ResponseEntity.status(401).body(
                    HubResponse.error(ex.getCode(), ex.getMessage())
            );
        }
    }
}
