package com.company.common.response.handler;

import com.company.common.response.config.ResponseProperties;
import com.company.common.response.dto.ApiResponse;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 統一成功回應包裝
 *
 * 自動將 controller 回傳值包裝成 ApiResponse 格式，排除路徑除外
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final ResponseProperties properties;
    private final JsonMapper jsonMapper;

    public GlobalResponseAdvice(ResponseProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 已經是 ApiResponse 就不再包裝
        return !ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 排除路徑不包裝
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String uri = servletRequest.getServletRequest().getRequestURI();
            for (String pattern : properties.getExcludePaths()) {
                if (pathMatcher.match(pattern, uri)) {
                    return body;
                }
            }
        }

        // 已經是 ApiResponse 就直接回傳
        if (body instanceof ApiResponse) {
            return body;
        }

        // String 類型需要特殊處理（Spring 用 StringHttpMessageConverter 而非 Jackson）
        if (body instanceof String) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return jsonMapper.writeValueAsString(ApiResponse.ok(body));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize ApiResponse", e);
            }
        }

        return ApiResponse.ok(body);
    }
}
