package com.company.common.log.interceptor;

import com.company.common.log.annotation.Loggable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 日誌攔截器 — 在 Controller 方法執行前讀取 {@link Loggable} 註解，
 * 將設定存入 request attribute 供 {@link com.company.common.log.filter.ApiLogFilter} 使用。
 *
 * <p>執行順序：
 * <pre>
 * Request → ApiLogFilter (doFilterInternal 前半) → ApiLogInterceptor.preHandle → Controller → ApiLogFilter (doFilterInternal 後半)
 * </pre>
 *
 * <p>存入 request attribute 的 key：
 * <ul>
 *   <li>{@code apiLog.logRequestBody} — boolean, 是否記錄請求 body</li>
 *   <li>{@code apiLog.logResponseBody} — boolean, 是否記錄回應 body</li>
 *   <li>{@code apiLog.maskFields} — String[], 要遮罩的欄位名稱</li>
 *   <li>{@code apiLog.slowThresholdMs} — long, 慢請求閾值</li>
 *   <li>{@code apiLog.handlerResolved} — boolean, 標記 interceptor 已處理過</li>
 * </ul>
 */
public class ApiLogInterceptor implements HandlerInterceptor {

    public static final String ATTR_LOG_REQUEST_BODY = "apiLog.logRequestBody";
    public static final String ATTR_LOG_RESPONSE_BODY = "apiLog.logResponseBody";
    public static final String ATTR_MASK_FIELDS = "apiLog.maskFields";
    public static final String ATTR_SLOW_THRESHOLD_MS = "apiLog.slowThresholdMs";
    public static final String ATTR_HANDLER_RESOLVED = "apiLog.handlerResolved";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 非 Controller 方法（例如靜態資源），標記不記錄
            return true;
        }

        // 方法層級 @Loggable 優先於類別層級
        Loggable loggable = handlerMethod.getMethodAnnotation(Loggable.class);
        if (loggable == null) {
            loggable = handlerMethod.getBeanType().getAnnotation(Loggable.class);
        }

        // 標記 interceptor 已處理過（Filter 用此判斷是否為 Controller 請求）
        request.setAttribute(ATTR_HANDLER_RESOLVED, true);

        if (loggable != null) {
            request.setAttribute(ATTR_LOG_REQUEST_BODY, loggable.logRequestBody());
            request.setAttribute(ATTR_LOG_RESPONSE_BODY, loggable.logResponseBody());
            request.setAttribute(ATTR_MASK_FIELDS, loggable.maskFields());
            request.setAttribute(ATTR_SLOW_THRESHOLD_MS, loggable.slowThresholdMs());
        } else {
            // 沒有 @Loggable — 使用預設行為
            request.setAttribute(ATTR_LOG_REQUEST_BODY, true);
            request.setAttribute(ATTR_LOG_RESPONSE_BODY, false);
            // maskFields 和 slowThresholdMs 不設定，讓 Filter 用全域配置
        }

        return true;
    }
}
