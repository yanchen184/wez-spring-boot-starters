package com.company.common.security.otp;

import com.company.common.security.dto.request.OtpLoginRequest;
import com.company.common.security.dto.request.OtpVerifyRequest;
import com.company.common.response.dto.ApiResponse;
import com.company.common.security.dto.response.OtpSetupResponse;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.otp.OtpService;
import com.company.common.security.otp.OtpService.OtpSetupResult;
import com.company.common.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OTP", description = "TOTP-based two-factor authentication")
@RestController
@RequestMapping("/api/auth/otp")
public class OtpController {

    private final OtpService otpService;
    private final AuthService authService;

    public OtpController(OtpService otpService, AuthService authService) {
        this.otpService = otpService;
        this.authService = authService;
    }

    @Operation(summary = "Setup OTP", description = "Generate a new TOTP secret for the authenticated user. Returns secret and QR code URI.")
    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<OtpSetupResponse>> setup(Authentication authentication) {
        OtpSetupResult result = otpService.setupOtp(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(new OtpSetupResponse(result.secret(), result.otpAuthUri())));
    }

    @Operation(summary = "Verify OTP setup", description = "Verify the code from authenticator app and enable OTP for the user.")
    @PostMapping("/verify-setup")
    public ResponseEntity<ApiResponse<Void>> verifySetup(
            @Valid @RequestBody OtpVerifyRequest request,
            Authentication authentication) {
        boolean valid = otpService.verifyAndEnableOtp(authentication.getName(), request.code());
        if (!valid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid OTP code"));
        }
        return ResponseEntity.ok(ApiResponse.ok("OTP enabled successfully", null));
    }

    @Operation(summary = "Verify OTP during login", description = "Verify OTP code after initial login returned requiresOtp=true. Returns JWT tokens on success.")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<TokenResponse>> verify(
            @Valid @RequestBody OtpLoginRequest request,
            HttpServletRequest httpRequest) {
        boolean valid = otpService.verifyOtp(request.username(), request.code());
        if (!valid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid OTP code"));
        }
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        TokenResponse token = authService.completeOtpLogin(request.username(), ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.ok("OTP verified", token));
    }

    @Operation(summary = "Disable OTP", description = "Disable OTP for the authenticated user.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> disable(Authentication authentication) {
        otpService.disableOtp(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("OTP disabled", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
