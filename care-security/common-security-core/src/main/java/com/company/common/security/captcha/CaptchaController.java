package com.company.common.security.captcha;

import com.company.common.response.dto.ApiResponse;
import com.company.common.security.dto.response.CaptchaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication and token management")
@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Operation(summary = "Generate CAPTCHA", description = "Returns a captcha ID and base64-encoded PNG image")
    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<CaptchaResponse>> generateCaptcha() {
        CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
        return ResponseEntity.ok(ApiResponse.ok(new CaptchaResponse(result.captchaId(), result.imageBase64())));
    }
}
