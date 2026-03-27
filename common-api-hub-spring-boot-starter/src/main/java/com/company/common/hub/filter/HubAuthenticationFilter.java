package com.company.common.hub.filter;

import com.company.common.hub.dto.HubAuthResult;
import com.company.common.hub.dto.HubResponse;
import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.exception.HubAuthException;
import com.company.common.hub.exception.HubTokenException;
import com.company.common.hub.repository.HubSetRepository;
import com.company.common.hub.service.HubAuthService;
import com.company.common.hub.service.HubLogService;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * API Hub 認證過濾器。
 *
 * <p>攔截所有已註冊的 HubSet URI，從 {@code X-Hub-Token} header 取得 Token，
 * 經由 {@link HubAuthService} 執行 4 層認證。
 *
 * <ul>
 *   <li>URI 不匹配任何 HubSet → 放行（不是 hub 管控的）</li>
 *   <li>匹配但無 Token header → 401</li>
 *   <li>認證成功 → 放行 + 記日誌</li>
 *   <li>認證失敗 → 回傳 JSON 錯誤 + 記日誌</li>
 * </ul>
 */
public class HubAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HubAuthenticationFilter.class);

    /** Token header 名稱。 */
    public static final String TOKEN_HEADER = "X-Hub-Token";

    private final HubAuthService hubAuthService;
    private final HubLogService hubLogService;
    private final HubSetRepository hubSetRepository;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public HubAuthenticationFilter(HubAuthService hubAuthService,
                                    HubLogService hubLogService,
                                    HubSetRepository hubSetRepository,
                                    ObjectMapper objectMapper) {
        this.hubAuthService = hubAuthService;
        this.hubLogService = hubLogService;
        this.hubSetRepository = hubSetRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = request.getRemoteAddr();

        // 檢查 URI 是否匹配任何啟用的 HubSet
        if (!isHubManagedUri(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(TOKEN_HEADER);
        long startTime = System.currentTimeMillis();

        if (token == null || token.isBlank()) {
            long elapsed = System.currentTimeMillis() - startTime;
            writeErrorResponse(response, HubResponseCode.AUTH_FAILED, "缺少 X-Hub-Token header");
            hubLogService.log(null, method, uri, null, clientIp, false,
                    HubResponseCode.AUTH_FAILED, null, "缺少 X-Hub-Token header", elapsed);
            return;
        }

        try {
            HubAuthResult result = hubAuthService.authenticate(
                    uri, method, null, null, token, clientIp
            );

            long elapsed = System.currentTimeMillis() - startTime;
            hubLogService.log(result.getHubUserSet(), method, uri, null, clientIp, true,
                    HubResponseCode.SUCCESS, null, null, elapsed);

            filterChain.doFilter(request, response);

        } catch (HubAuthException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("認證失敗: uri={}, ip={}, code={}, message={}",
                    uri, clientIp, ex.getCode(), ex.getMessage());
            writeErrorResponse(response, ex.getCode(), ex.getMessage());
            hubLogService.log(null, method, uri, null, clientIp, false,
                    ex.getCode(), null, ex.getMessage(), elapsed);

        } catch (HubTokenException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("Token 驗證失敗: uri={}, ip={}, code={}, message={}",
                    uri, clientIp, ex.getCode(), ex.getMessage());
            writeErrorResponse(response, ex.getCode(), ex.getMessage());
            hubLogService.log(null, method, uri, null, clientIp, false,
                    ex.getCode(), null, ex.getMessage(), elapsed);
        }
    }

    /**
     * 檢查 URI 是否在 Hub 管控範圍內。
     */
    private boolean isHubManagedUri(String uri) {
        List<HubSet> enabledSets = hubSetRepository.findByEnabledTrue();
        return enabledSets.stream()
                .anyMatch(set -> pathMatcher.match(set.getUri(), uri));
    }

    /**
     * 寫入 JSON 錯誤回應。
     */
    private void writeErrorResponse(HttpServletResponse response,
                                     String code, String message) throws IOException {
        int httpStatus = resolveHttpStatus(code);
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        HubResponse<Void> body = HubResponse.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * 根據錯誤代碼前 3 碼決定 HTTP Status。
     */
    private int resolveHttpStatus(String code) {
        if (code == null || code.length() < 3) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        return switch (code.substring(0, 3)) {
            case "401" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "422" -> 422;
            case "500" -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
    }
}
