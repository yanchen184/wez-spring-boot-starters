package com.company.common.response.handler;

import com.company.common.response.code.CommonErrorCode;
import com.company.common.response.config.ResponseProperties;
import com.company.common.response.dto.ApiResponse;
import com.company.common.response.dto.FieldError;
import com.company.common.response.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.AntPathMatcher;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用全局異常處理器（最低優先級）
 *
 * 處理通用異常，安全相關異常由各 starter 自行處理（使用更高優先級的 @Order）
 * 支援路徑排除（預設排除 /actuator/**），排除的路徑交由 Spring 預設機制處理
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final ResponseProperties properties;

    public GlobalExceptionHandler(ResponseProperties properties) {
        this.properties = properties;
    }

    /**
     * 檢查請求路徑是否在排除清單中，若是則 re-throw 交給 Spring 預設處理
     */
    private void reThrowIfExcluded(HttpServletRequest request, Exception ex) throws Exception {
        String uri = request.getRequestURI();
        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, uri)) {
                throw ex;
            }
        }
    }

    /**
     * 處理業務異常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        log.warn("BusinessException: {} {} - code={} message={}",
                request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * 處理參數驗證異常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> FieldError.of(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .collect(Collectors.toList());

        log.warn("ValidationException: {} {} - errors={}",
                request.getMethod(), request.getRequestURI(), errors);

        ApiResponse<Void> response = ApiResponse.validationError(errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 處理綁定異常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> FieldError.of(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .collect(Collectors.toList());

        log.warn("BindException: {} {} - errors={}",
                request.getMethod(), request.getRequestURI(), errors);

        ApiResponse<Void> response = ApiResponse.validationError(errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 處理缺少請求參數
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        log.warn("MissingParameter: {} {} - param={}",
                request.getMethod(), request.getRequestURI(), ex.getParameterName());

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.MISSING_PARAMETER,
                String.format("缺少必要參數: %s", ex.getParameterName()));
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 處理參數類型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        log.warn("TypeMismatch: {} {} - param={} value={}",
                request.getMethod(), request.getRequestURI(),
                ex.getName(), ex.getValue());

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.INVALID_PARAMETER,
                String.format("參數 '%s' 類型錯誤", ex.getName()));
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 處理請求體無法讀取
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        log.warn("MessageNotReadable: {} {} - {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.BAD_REQUEST, "請求體格式錯誤");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 處理不支援的 HTTP 方法
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        log.warn("MethodNotSupported: {} {} - supported={}",
                request.getMethod(), request.getRequestURI(), ex.getSupportedMethods());

        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 處理不支援的 Content-Type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        log.warn("MediaTypeNotSupported: {} {} - contentType={}",
                request.getMethod(), request.getRequestURI(), ex.getContentType());

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.BAD_REQUEST,
                String.format("不支援的 Content-Type: %s", ex.getContentType()));
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * 處理 404 - 找不到處理器
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            Exception ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        log.warn("NotFound: {} {}", request.getMethod(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 處理 IllegalArgumentException（參數不合法）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        log.warn("IllegalArgument: {} {} - {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 處理所有其他未知異常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex, HttpServletRequest request) throws Exception {

        reThrowIfExcluded(request, ex);

        // 記錄完整的錯誤堆疊
        log.error("UnhandledException: {} {} - {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        // 生產環境不暴露詳細錯誤訊息
        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.INTERNAL_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
