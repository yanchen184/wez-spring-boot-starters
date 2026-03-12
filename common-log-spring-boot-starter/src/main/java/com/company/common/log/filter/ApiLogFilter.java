package com.company.common.log.filter;

import com.company.common.log.config.LogProperties;
import com.company.common.log.interceptor.ApiLogInterceptor;
import com.company.common.log.util.MaskUtils;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

/**
 * API 自動日誌 Filter
 *
 * <p>日誌格式（箭頭式，與原 AOP 版本完全相容）：
 * <pre>
 * --> POST /api/auth/login body={username:admin, password:***} user=anonymous
 * <-- 200 POST /api/auth/login 271ms
 * <-- 200 GET /api/users 1ms body={...}        (logResponseBody=true)
 * <-- 200 POST /api/slow 5123ms [SLOW]         (超過 slow threshold)
 * </pre>
 *
 * <p>traceId/spanId 由 {@link TracingFilter} + MDC 自動顯示，不重複印。
 *
 * <p>設計：
 * <ul>
 *   <li>Filter 負責計時、記錄 --> / <-- log</li>
 *   <li>{@link com.company.common.log.interceptor.ApiLogInterceptor} 負責讀取
 *       {@code @Loggable} 註解並存入 request attribute</li>
 *   <li>Filter 在 chain 完成後讀取 request attribute 來決定行為</li>
 * </ul>
 *
 * <p>執行流程：
 * <pre>
 * Request → ApiLogFilter.doFilterInternal
 *         → wrap request/response
 *         → filterChain.doFilter(...)
 *             → ApiLogInterceptor.preHandle（設定 request attributes）
 *             → Controller
 *             → ApiLogInterceptor.afterCompletion
 *         → 讀取 attributes，記錄 --> 和 <-- log
 * </pre>
 *
 * <p>注意：--> log 在 filterChain.doFilter 之後印出（因為需要等 Interceptor 設定好 attributes）。
 * 雖然時序上是「先印 --> 再印 <--」，但兩者都在 response 返回前完成。
 */
public class ApiLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiLogFilter.class);

    /**
     * 靜態資源副檔名 — 跳過這些路徑，不記錄 log
     */
    private static final Set<String> STATIC_EXTENSIONS = Set.of(
            ".html", ".htm", ".css", ".js", ".jsx", ".ts", ".tsx",
            ".ico", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp",
            ".woff", ".woff2", ".ttf", ".eot", ".map"
    );

    private final ObjectMapper objectMapper;
    private final LogProperties logProperties;

    public ApiLogFilter(ObjectMapper objectMapper, LogProperties logProperties) {
        this.objectMapper = objectMapper;
        this.logProperties = logProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 跳過靜態資源
        for (String ext : STATIC_EXTENSIONS) {
            if (uri.endsWith(ext)) {
                return true;
            }
        }

        // 跳過 actuator 及其他排除路徑
        for (String pattern : logProperties.getExcludePatterns()) {
            if (matchesPattern(uri, pattern)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String httpMethod = request.getMethod();
        String uri = request.getRequestURI();
        String user = getUsername(request);

        // 包裝 request 以便重複讀取 body（Spring 7.x 要求指定 contentCacheLimit）
        ContentCachingRequestWrapper wrappedRequest =
                (request instanceof ContentCachingRequestWrapper)
                        ? (ContentCachingRequestWrapper) request
                        : new ContentCachingRequestWrapper(request, 10240);

        // 包裝 response 以便讀取 response body
        ContentCachingResponseWrapper wrappedResponse =
                (response instanceof ContentCachingResponseWrapper)
                        ? (ContentCachingResponseWrapper) response
                        : new ContentCachingResponseWrapper(response);

        // --> 請求 log（在 doFilter 之前印，確保時序正確）
        // 此時尚無 @Loggable 資訊，先用預設行為印出基本請求 log
        // GET/DELETE 可以印 query params，POST/PUT body 要等 doFilter 後才讀得到
        log.info("--> {} {} user={}", httpMethod, uri, user);

        try {
            // 執行 filter chain（包含 Interceptor + Controller）
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // Interceptor 已在 preHandle 設定好 attributes
            boolean handlerResolved = Boolean.TRUE.equals(
                    wrappedRequest.getAttribute(ApiLogInterceptor.ATTR_HANDLER_RESOLVED));

            if (handlerResolved) {
                // 讀取 @Loggable 設定
                boolean logRequestBody = resolveBoolean(wrappedRequest,
                        ApiLogInterceptor.ATTR_LOG_REQUEST_BODY, true);
                boolean logResponseBody = resolveLogResponseBody(wrappedRequest);
                Set<String> maskFields = resolveMaskFields(wrappedRequest);
                long slowThreshold = resolveSlowThreshold(wrappedRequest);

                // 補印 request body（POST/PUT/PATCH 才有 body）
                if (logRequestBody) {
                    String bodyStr = extractRequestBodyFromWrapper(wrappedRequest, maskFields);
                    if (bodyStr != null && !bodyStr.isEmpty()) {
                        log.info("    body={}", bodyStr);
                    }
                }

                // <-- 回應 log
                long cost = System.currentTimeMillis() - startTime;
                int status = wrappedResponse.getStatus();
                logResponseLine(wrappedResponse, httpMethod, uri, cost, status,
                        slowThreshold, logResponseBody, maskFields);
            } else {
                // 非 Controller 請求（不該到這裡，因為 shouldNotFilter 已過濾）
                long cost = System.currentTimeMillis() - startTime;
                log.info("<-- {} {} {} {}ms", wrappedResponse.getStatus(), httpMethod, uri, cost);
            }

            // 重要：將 response body 寫回原始 response
            wrappedResponse.copyBodyToResponse();
        }
    }

    // ==================== --> 請求 log ====================

    private void logRequestLine(ContentCachingRequestWrapper request,
                                String httpMethod, String uri, String user,
                                boolean logRequestBody, Set<String> maskFields) {
        String bodyStr = null;
        if (logRequestBody) {
            bodyStr = extractBody(request, httpMethod, maskFields);
        }

        if (bodyStr != null && !bodyStr.isEmpty()) {
            log.info("--> {} {} body={} user={}", httpMethod, uri, bodyStr, user);
        } else {
            log.info("--> {} {} user={}", httpMethod, uri, user);
        }
    }

    /**
     * 提取請求參數：GET/DELETE 取 query string，POST/PUT/PATCH 取 request body
     */
    private String extractBody(ContentCachingRequestWrapper request,
                                String httpMethod, Set<String> maskFields) {
        try {
            if ("GET".equalsIgnoreCase(httpMethod) || "DELETE".equalsIgnoreCase(httpMethod)) {
                return MaskUtils.maskQueryParams(request.getParameterMap(), maskFields);
            } else {
                return extractRequestBodyFromWrapper(request, maskFields);
            }
        } catch (Exception e) {
            log.debug("提取 request body 失敗: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 從 ContentCachingRequestWrapper 讀取 body 並遮罩敏感欄位
     */
    @SuppressWarnings("unchecked")
    private String extractRequestBodyFromWrapper(ContentCachingRequestWrapper request,
                                                  Set<String> maskFields) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        try {
            String rawBody = new String(content, StandardCharsets.UTF_8);
            if (rawBody.isBlank()) {
                return null;
            }
            // 嘗試 JSON 解析並遮罩
            Object parsed = objectMapper.readValue(rawBody, Object.class);
            return MaskUtils.maskAndSerialize(parsed, maskFields, objectMapper);
        } catch (Exception e) {
            log.debug("解析 request body JSON 失敗，原樣輸出: {}", e.getMessage());
            // 非 JSON body，直接返回原始內容
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    // ==================== <-- 回應 log ====================

    private void logResponseLine(ContentCachingResponseWrapper response,
                                  String httpMethod, String uri, long cost, int status,
                                  long slowThreshold, boolean logResponseBody,
                                  Set<String> maskFields) {
        String bodyStr = buildResponseBodyString(response, logResponseBody, maskFields);
        boolean isSlow = slowThreshold > 0 && cost >= slowThreshold;

        if (isSlow) {
            log.warn("<-- {} {} {} {}ms [SLOW]{}", status, httpMethod, uri, cost, bodyStr);
        } else {
            log.info("<-- {} {} {} {}ms{}", status, httpMethod, uri, cost, bodyStr);
        }
    }

    /**
     * 組裝 response body 字串（含遮罩），若不需要印則回傳空字串
     */
    @SuppressWarnings("unchecked")
    private String buildResponseBodyString(ContentCachingResponseWrapper response,
                                            boolean logResponseBody, Set<String> maskFields) {
        if (!logResponseBody) {
            return "";
        }
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length == 0) {
                return "";
            }
            String rawBody = new String(content, StandardCharsets.UTF_8);
            if (rawBody.isBlank()) {
                return "";
            }

            // 嘗試 JSON 解析並遮罩
            String serialized;
            try {
                Object parsed = objectMapper.readValue(rawBody, Object.class);
                serialized = MaskUtils.maskAndSerialize(parsed, maskFields, objectMapper);
            } catch (Exception e) {
                // 非 JSON，原樣輸出
                serialized = rawBody;
            }

            // 截斷過長的 response body
            int maxLen = logProperties.getMaxResponseLength();
            if (serialized.length() > maxLen) {
                serialized = serialized.substring(0, maxLen) + "...";
            }
            return " body=" + serialized;
        } catch (Exception e) {
            log.debug("序列化 response body 失敗: {}", e.getMessage());
            return "";
        }
    }

    // ==================== 解析 request attributes ====================

    private boolean resolveBoolean(HttpServletRequest request, String attrName, boolean defaultValue) {
        Object value = request.getAttribute(attrName);
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }

    /**
     * 解析是否記錄 response body：@Loggable 設定優先，否則用全域配置
     */
    private boolean resolveLogResponseBody(HttpServletRequest request) {
        Object value = request.getAttribute(ApiLogInterceptor.ATTR_LOG_RESPONSE_BODY);
        if (value instanceof Boolean b) {
            return b;
        }
        return logProperties.isLogResponseBody();
    }

    /**
     * 解析遮罩欄位：@Loggable 設定優先，否則用全域配置
     */
    private Set<String> resolveMaskFields(HttpServletRequest request) {
        Object value = request.getAttribute(ApiLogInterceptor.ATTR_MASK_FIELDS);
        if (value instanceof String[] fields) {
            return Set.of(fields);
        }
        return logProperties.getMaskFields();
    }

    /**
     * 解析慢請求閾值：@Loggable 設定優先（正數），否則用全域配置
     */
    private long resolveSlowThreshold(HttpServletRequest request) {
        Object value = request.getAttribute(ApiLogInterceptor.ATTR_SLOW_THRESHOLD_MS);
        if (value instanceof Long threshold && threshold > 0) {
            return threshold;
        }
        return logProperties.getSlowThresholdMs();
    }

    // ==================== 工具方法 ====================

    private String getUsername(HttpServletRequest request) {
        try {
            Principal principal = request.getUserPrincipal();
            if (principal != null) {
                return principal.getName();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return "anonymous";
    }

    /**
     * 簡易 URL pattern 匹配，支援：
     * - 精確匹配：/health
     * - 萬用字元結尾：/actuator/**
     */
    private boolean matchesPattern(String uri, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return uri.equals(prefix) || uri.startsWith(prefix + "/");
        }
        return uri.equals(pattern);
    }
}
