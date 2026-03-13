package com.company.common.security.cert;

import com.company.common.response.dto.ApiResponse;
import com.company.common.security.cert.exception.MoicaLoginException;
import com.company.common.security.dto.request.CertLoginRequest;
import com.company.common.security.dto.response.LoginTokenResponse;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.entity.SaUser;
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

import java.security.cert.X509Certificate;

/**
 * REST controller for MOICA citizen digital certificate authentication.
 * <p>
 * Flow:
 * 1. Client calls GET /api/auth/cert/login-token to obtain a login token
 * 2. Client signs the login token with their citizen certificate (PKCS#7)
 * 3. Client calls POST /api/auth/cert/login with the loginToken + base64Data (PKCS#7)
 * 4. Server verifies everything and returns JWT tokens
 */
@Tag(name = "Citizen Certificate", description = "MOICA Citizen Digital Certificate (自然人憑證) authentication")
@RestController
@RequestMapping("/api/auth/cert")
public class CitizenCertController {

    private static final Logger log = LoggerFactory.getLogger(CitizenCertController.class);

    private final LoginTokenService loginTokenService;
    private final MoicaCertService moicaCertService;
    private final CitizenCertUserSyncService citizenCertUserSyncService;
    private final AuthService authService;
    private final AuditService auditService;

    public CitizenCertController(LoginTokenService loginTokenService,
                                  MoicaCertService moicaCertService,
                                  CitizenCertUserSyncService citizenCertUserSyncService,
                                  AuthService authService,
                                  AuditService auditService) {
        this.loginTokenService = loginTokenService;
        this.moicaCertService = moicaCertService;
        this.citizenCertUserSyncService = citizenCertUserSyncService;
        this.authService = authService;
        this.auditService = auditService;
    }

    @Operation(summary = "Get login token",
            description = "Generate a random login token for the client to sign with their citizen certificate via PKCS#7.")
    @GetMapping("/login-token")
    public ResponseEntity<ApiResponse<LoginTokenResponse>> getLoginToken() {
        String loginToken = loginTokenService.generateLoginToken();
        return ResponseEntity.ok(ApiResponse.ok(new LoginTokenResponse(loginToken)));
    }

    @Operation(summary = "Login with citizen certificate (PKCS#7)",
            description = "Authenticate by providing the loginToken and base64-encoded PKCS#7 signed data.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody CertLoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            // 1. Consume login token (one-time use, prevents replay)
            boolean tokenValid = loginTokenService.consumeLoginToken(request.loginToken());
            if (!tokenValid) {
                auditService.logEvent("LOGIN_CERT_FAIL", null, null, ipAddress, userAgent,
                        "Login token expired or already consumed");
                throw new MoicaLoginException("Login token expired or already consumed");
            }

            // 2. Parse PKCS#7 envelope
            Pkcs7Utils pkcs7;
            try {
                pkcs7 = new Pkcs7Utils(request.base64Data(), request.loginToken());
            } catch (IllegalArgumentException e) {
                auditService.logEvent("LOGIN_CERT_FAIL", null, null, ipAddress, userAgent,
                        "Invalid PKCS#7 data: " + e.getMessage());
                throw new MoicaLoginException("Invalid PKCS#7 data: " + e.getMessage(), e);
            }

            // 3. Verify PKCS#7 signature (proves client possesses the private key)
            if (!pkcs7.valid()) {
                auditService.logEvent("LOGIN_CERT_FAIL", null, null, ipAddress, userAgent,
                        "PKCS#7 signature verification failed");
                throw new MoicaLoginException("PKCS#7 signature verification failed");
            }

            // 4. Extract certificate
            X509Certificate certificate = pkcs7.getCert();
            if (certificate == null) {
                throw new MoicaLoginException("No certificate found in PKCS#7 envelope");
            }

            // 5. Full certificate verification (validity, chain, revocation)
            moicaCertService.fullVerify(certificate);

            // 6. Extract identity information
            MoicaCertUtils verifier = moicaCertService.createVerifier(certificate);
            String cname = verifier.getSubjectCName();
            String last4IDNO = verifier.getExtLast4IDNO();
            String cardSN = pkcs7.getCardSN();
            String certSN = verifier.getSN();

            log.info("MOICA cert login: cname={}, last4IDNO={}, cardSN={}, certSN={}",
                    cname, last4IDNO != null ? "****" : "null", cardSN, certSN);

            if (cname == null || cname.isBlank()) {
                throw new MoicaLoginException("Cannot extract identity from certificate (CN is empty)");
            }

            // 7. Sync user (lookup by cname + last4IDNO, or create new)
            SaUser user = citizenCertUserSyncService.syncUserByCnameAndIdno(cname, last4IDNO, cname);

            // 8. Generate JWT tokens
            String username = user.getUsername();
            auditService.logEvent("LOGIN_CERT", username, null, ipAddress, userAgent,
                    String.format("MOICA cert login: cname=%s, certSN=%s, cardSN=%s", cname, certSN, cardSN));

            TokenResponse token = authService.completeCertLogin(username, ipAddress, userAgent);
            return ResponseEntity.ok(ApiResponse.ok("Certificate login successful", token));

        } catch (MoicaLoginException e) {
            // Re-throw as-is (it extends BadCredentialsException)
            throw e;
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Certificate login error: {} ({})", e.getMessage(), e.getClass().getSimpleName(), e);
            throw new MoicaLoginException("Certificate authentication failed: " + e.getMessage(), e);
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
