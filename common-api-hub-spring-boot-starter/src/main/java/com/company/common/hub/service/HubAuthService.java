package com.company.common.hub.service;

import com.company.common.hub.dto.HubAuthResult;
import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import com.company.common.hub.exception.HubAuthException;
import com.company.common.hub.repository.HubSetRepository;
import com.company.common.hub.repository.HubUserRepository;
import com.company.common.hub.repository.HubUserSetRepository;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.AntPathMatcher;

import java.time.LocalDate;
import java.util.List;

/**
 * 4 層認證核心服務。
 *
 * <p>認證流程：
 * <ol>
 *   <li>Layer 1: URI 匹配 — 查 HubSet，支援 Ant 風格</li>
 *   <li>Layer 2: 認證 — 帳密（BCrypt）或 Token（JWT）</li>
 *   <li>Layer 3: 授權 — HubUserSet 啟用 + 有效期 + 認證策略</li>
 *   <li>Layer 4: IP 白名單 — IpWhitelistService</li>
 * </ol>
 */
public class HubAuthService {

    private static final Logger log = LoggerFactory.getLogger(HubAuthService.class);

    private final HubSetRepository hubSetRepository;
    private final HubUserRepository hubUserRepository;
    private final HubUserSetRepository hubUserSetRepository;
    private final HubTokenService hubTokenService;
    private final IpWhitelistService ipWhitelistService;
    private final PasswordEncoder passwordEncoder;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public HubAuthService(HubSetRepository hubSetRepository,
                          HubUserRepository hubUserRepository,
                          HubUserSetRepository hubUserSetRepository,
                          HubTokenService hubTokenService,
                          IpWhitelistService ipWhitelistService,
                          PasswordEncoder passwordEncoder) {
        this.hubSetRepository = hubSetRepository;
        this.hubUserRepository = hubUserRepository;
        this.hubUserSetRepository = hubUserSetRepository;
        this.hubTokenService = hubTokenService;
        this.ipWhitelistService = ipWhitelistService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 執行 4 層認證。
     *
     * @param uri       請求 URI
     * @param method    HTTP 方法
     * @param username  帳號（帳密認證時必填）
     * @param password  密碼（帳密認證時必填）
     * @param token     JWT Token（Token 認證時必填）
     * @param clientIp  客戶端 IP
     * @return 認證結果
     * @throws HubAuthException 認證失敗
     */
    public HubAuthResult authenticate(String uri, String method,
                                       String username, String password,
                                       String token, String clientIp) {
        // Layer 1: URI 匹配
        HubSet hubSet = matchUri(uri);

        // 判斷認證方式
        boolean usePassword = username != null && password != null;
        boolean useToken = token != null;

        if (!usePassword && !useToken) {
            throw new HubAuthException(HubResponseCode.AUTH_FAILED, "未提供認證資訊");
        }

        HubUser hubUser;
        boolean authenticatedByToken = false;

        if (useToken) {
            // Layer 2: Token 認證
            hubUser = authenticateByToken(token, hubSet);
            authenticatedByToken = true;
        } else {
            // Layer 2: 帳密認證
            hubUser = authenticateByPassword(username, password);
        }

        // Layer 3: 授權檢查
        HubUserSet hubUserSet = authorize(hubUser, hubSet, authenticatedByToken);

        // Layer 4: IP 白名單
        checkIpWhitelist(hubUser, clientIp);

        log.info("認證成功: user={}, uri={}, method={}, ip={}",
                hubUser.getUsername(), uri, method, clientIp);

        return new HubAuthResult(hubUser, hubSet, hubUserSet);
    }

    /**
     * Layer 1: URI 匹配。
     * 從所有啟用的 HubSet 中找到匹配的 URI pattern。
     */
    private HubSet matchUri(String uri) {
        List<HubSet> enabledSets = hubSetRepository.findByEnabledTrue();

        return enabledSets.stream()
                .filter(set -> pathMatcher.match(set.getUri(), uri))
                .findFirst()
                .orElseThrow(() -> new HubAuthException(
                        HubResponseCode.API_SET_DISABLED,
                        "URI 不在管控範圍或 API 設定已停用: " + uri
                ));
    }

    /**
     * Layer 2: 帳密認證。
     */
    private HubUser authenticateByPassword(String username, String password) {
        HubUser hubUser = hubUserRepository.findByUsername(username)
                .orElseThrow(() -> new HubAuthException(
                        HubResponseCode.AUTH_FAILED, "帳號不存在: " + username
                ));

        if (!Boolean.TRUE.equals(hubUser.getEnabled())) {
            throw new HubAuthException(HubResponseCode.AUTH_FAILED, "帳號已停用: " + username);
        }

        if (!passwordEncoder.matches(password, hubUser.getPassword())) {
            throw new HubAuthException(HubResponseCode.AUTH_FAILED, "密碼錯誤");
        }

        return hubUser;
    }

    /**
     * Layer 2: Token 認證。
     */
    private HubUser authenticateByToken(String token, HubSet hubSet) {
        // validateToken 會拋出 HubTokenException（422001/422002），直接讓它穿透
        Claims claims = hubTokenService.validateToken(token);

        if (hubTokenService.isBlacklisted(token)) {
            throw new HubAuthException(HubResponseCode.TOKEN_INVALID, "Token 已被列入黑名單");
        }

        Long userId = claims.get("userId", Long.class);
        return hubUserRepository.findById(userId)
                .orElseThrow(() -> new HubAuthException(
                        HubResponseCode.AUTH_FAILED, "Token 中的使用者不存在: userId=" + userId
                ));
    }

    /**
     * Layer 3: 授權檢查。
     * 驗證 HubUserSet 啟用、有效期、認證策略。
     */
    private HubUserSet authorize(HubUser hubUser, HubSet hubSet, boolean authenticatedByToken) {
        HubUserSet hubUserSet = hubUserSetRepository
                .findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet)
                .orElseThrow(() -> new HubAuthException(
                        HubResponseCode.AUTH_FAILED, "無此 API 的授權設定"
                ));

        // 有效期檢查
        LocalDate today = LocalDate.now();
        if (hubUserSet.getVerifyDts() != null && today.isBefore(hubUserSet.getVerifyDts())) {
            throw new HubAuthException(HubResponseCode.AUTH_FAILED, "授權尚未生效");
        }
        if (hubUserSet.getVerifyDte() != null && today.isAfter(hubUserSet.getVerifyDte())) {
            throw new HubAuthException(HubResponseCode.AUTH_FAILED, "授權已過期");
        }

        // 認證策略檢查
        if (authenticatedByToken && !Boolean.TRUE.equals(hubUserSet.getJwtTokenVerify())) {
            throw new HubAuthException(HubResponseCode.AUTH_FAILED, "此授權不允許 Token 驗證");
        }
        if (!authenticatedByToken && !Boolean.TRUE.equals(hubUserSet.getUserVerify())) {
            throw new HubAuthException(HubResponseCode.AUTH_FAILED, "此授權不允許帳密驗證");
        }

        return hubUserSet;
    }

    /**
     * Layer 4: IP 白名單檢查。
     */
    private void checkIpWhitelist(HubUser hubUser, String clientIp) {
        if (!ipWhitelistService.isAllowed(clientIp, hubUser.getVerifyIp())) {
            throw new HubAuthException(HubResponseCode.IP_DENIED,
                    "IP 不在白名單: " + clientIp);
        }
    }
}
