package com.company.common.security.controller;

import com.company.common.security.service.CaptchaService;

import com.company.common.response.dto.ApiResponse;
import com.company.common.security.dto.response.CaptchaAudioResponse;
import com.company.common.security.dto.response.CaptchaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public CaptchaResponse generateCaptcha() {
        CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
        return new CaptchaResponse(result.captchaId(), result.imageBase64());
    }

    @Operation(summary = "Get CAPTCHA audio", description = "Returns a WAV audio base64 for accessibility")
    @GetMapping("/captcha/audio/{captchaId}")
    public ResponseEntity<?> getCaptchaAudio(@PathVariable String captchaId) {
        if (!captchaService.isAudioEnabled()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Audio CAPTCHA is not enabled"));
        }

        String audioBase64 = captchaService.generateAudioBase64(captchaId);
        if (audioBase64 == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("CAPTCHA not found or expired"));
        }

        return ResponseEntity.ok(ApiResponse.ok(new CaptchaAudioResponse(audioBase64)));
    }
}
