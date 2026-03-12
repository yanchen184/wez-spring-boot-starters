package com.company.common.log.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 確保每個 HTTP 請求都有 traceId/spanId 寫入 MDC
 *
 * 使用 ObjectProvider 延遲取得 Tracer，避免 bean 建立順序問題。
 * 在 filter chain 最前面執行，確保所有 log 都帶 traceId。
 */
public class TracingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TracingFilter.class);

    private final ObjectProvider<Tracer> tracerProvider;
    private volatile boolean debugLogged = false;

    public TracingFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Tracer tracer = tracerProvider.getIfAvailable();

        // 只印一次 debug log
        if (!debugLogged) {
            debugLogged = true;
            log.info("[TracingFilter] Tracer={}, class={}",
                    tracer != null ? "available" : "NULL",
                    tracer != null ? tracer.getClass().getName() : "N/A");
        }

        if (tracer == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Span currentSpan = tracer.currentSpan();

        if (currentSpan != null) {
            setMdc(currentSpan);
            try {
                filterChain.doFilter(request, response);
            } finally {
                clearMdc();
            }
        } else {
            Span newSpan = tracer.nextSpan().name(request.getMethod() + " " + request.getRequestURI()).start();
            try (Tracer.SpanInScope scope = tracer.withSpan(newSpan)) {
                setMdc(newSpan);
                filterChain.doFilter(request, response);
            } finally {
                newSpan.end();
                clearMdc();
            }
        }
    }

    private void setMdc(Span span) {
        MDC.put("traceId", span.context().traceId());
        MDC.put("spanId", span.context().spanId());
    }

    private void clearMdc() {
        MDC.remove("traceId");
        MDC.remove("spanId");
    }
}
