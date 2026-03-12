package com.company.common.security.cert;

import com.company.common.response.dto.ApiResponse;
import com.company.common.security.cert.CertChallengeService.ChallengeResult;
import com.company.common.security.cert.CertVerificationService.CertVerifyResult;
import com.company.common.security.dto.request.CertLoginRequest;
import com.company.common.security.dto.response.CertChallengeResponse;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.entity.SaUser;
import com.company.common.security.security.CustomUserDetails;
import com.company.common.security.security.CustomUserDetailsService;
import com.company.common.security.service.AuditService;
import com.company.common.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;

@Tag(name = "Citizen Certificate", description = "Citizen Digital Certificate (自然人憑證) authentication")
@RestController
@RequestMapping("/api/auth/cert")
public class CitizenCertController {

    private static final Logger log = LoggerFactory.getLogger(CitizenCertController.class);

    private final CertChallengeService certChallengeService;
    private final CertVerificationService certVerificationService;
    private final CitizenCertUserSyncService citizenCertUserSyncService;
    private final AuthService authService;
    private final AuditService auditService;

    public CitizenCertController(CertChallengeService certChallengeService,
                                  CertVerificationService certVerificationService,
                                  CitizenCertUserSyncService citizenCertUserSyncService,
                                  AuthService authService,
                                  AuditService auditService) {
        this.certChallengeService = certChallengeService;
        this.certVerificationService = certVerificationService;
        this.citizenCertUserSyncService = citizenCertUserSyncService;
        this.authService = authService;
        this.auditService = auditService;
    }

    @Operation(summary = "Get challenge", description = "Generate a random challenge nonce for the client to sign with their citizen certificate.")
    @GetMapping("/challenge")
    public ResponseEntity<ApiResponse<CertChallengeResponse>> getChallenge() {
        ChallengeResult challenge = certChallengeService.generateChallenge();
        return ResponseEntity.ok(ApiResponse.ok(
                new CertChallengeResponse(challenge.challengeId(), challenge.nonce())));
    }

    @Operation(summary = "Login with citizen certificate", description = "Authenticate by providing the signed challenge along with the X.509 certificate.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody CertLoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            // 1. Consume challenge (one-time use, prevents replay)
            Optional<String> nonceOpt = certChallengeService.consumeChallenge(request.challengeId());
            if (nonceOpt.isEmpty()) {
                auditService.logEvent("LOGIN_CERT_FAIL", null, null, ipAddress, userAgent,
                        "Challenge expired or already consumed");
                throw new BadCredentialsException("Challenge expired or already consumed");
            }
            String nonce = nonceOpt.get();

            // 2. Parse X.509 certificate
            byte[] certBytes = Base64.getDecoder().decode(request.certificate());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(certBytes));

            // 3. Validate certificate is not expired
            if (!certVerificationService.isCertificateValid(certificate)) {
                auditService.logEvent("LOGIN_CERT_FAIL", null, null, ipAddress, userAgent,
                        "Certificate expired or not yet valid");
                throw new BadCredentialsException("Certificate is expired or not yet valid");
            }

            // 4. Verify signature
            byte[] signatureBytes = Base64.getDecoder().decode(request.signature());
            CertVerifyResult verifyResult = certVerificationService.verifySignature(
                    certificate, nonce.getBytes(java.nio.charset.StandardCharsets.UTF_8), signatureBytes);

            if (!verifyResult.valid()) {
                auditService.logEvent("LOGIN_CERT_FAIL", null, null, ipAddress, userAgent,
                        "Signature verification failed");
                throw new BadCredentialsException("Invalid certificate signature");
            }

            // 5. Extract citizen ID
            String citizenId = certVerificationService.extractCitizenId(certificate);
            if (citizenId == null || citizenId.isBlank()) {
                throw new BadCredentialsException("Cannot extract citizen ID from certificate");
            }

            // 6. Sync user (create if not exists, use citizenId as fallback display name)
            SaUser user = citizenCertUserSyncService.syncUser(citizenId, citizenId);

            // 7. Generate JWT tokens
            auditService.logEvent("LOGIN_CERT", citizenId, null, ipAddress, userAgent,
                    "Citizen certificate login successful");

            TokenResponse token = authService.completeCertLogin(citizenId, ipAddress, userAgent);
            return ResponseEntity.ok(ApiResponse.ok("Certificate login successful", token));

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Certificate login error: {} ({})", e.getMessage(), e.getClass().getSimpleName(), e);
            throw new BadCredentialsException("Certificate authentication failed: " + e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
