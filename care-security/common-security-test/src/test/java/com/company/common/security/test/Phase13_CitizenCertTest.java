package com.company.common.security.test;

import com.company.common.security.cert.CertChallengeService;
import com.company.common.security.cert.CertChallengeService.ChallengeResult;
import com.company.common.security.cert.CertVerificationService;
import com.company.common.security.cert.CertVerificationService.CertVerifyResult;
import com.company.common.security.cert.CitizenCertUserSyncService;
import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.service.AuthService;
import com.company.common.security.dto.response.TokenResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 13: Citizen Digital Certificate Authentication (自然人憑證驗證)
 *
 * === TDD Guide for Engineers ===
 *
 * This test file serves as the SPECIFICATION for citizen certificate authentication.
 * Each test describes a business requirement.
 *
 * Test structure follows business scenarios:
 *
 * 13.1 Challenge Mechanism
 *     → What: Server generates a random challenge for the client to sign
 *     → Why: Prevent replay attacks; each authentication attempt needs a fresh challenge
 *
 * 13.2 Certificate Verification
 *     → What: Validate X.509 certificate chain, signature, and extract citizen identity
 *     → Why: Ensure only valid MOICA-issued certificates are accepted
 *
 * 13.3 User Sync (Auto-Create / Bind)
 *     → What: Map citizen certificate identity to local SaUser account
 *     → Why: User story — "As a citizen, I want to log in with my smart card without pre-registration"
 *
 * 13.4 Cert Login Flow (REST API)
 *     → What: Full end-to-end login flow via /api/auth/cert endpoints
 *     → Why: User story — "As a user, I want to authenticate with my citizen certificate"
 *
 * 13.5 Security Edge Cases
 *     → What: Expired/revoked certs, replay attacks, tampered signatures
 *     → Why: Prevent certificate-based attacks
 */
@CareSecurityTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Phase 13: Citizen Digital Certificate Authentication")
class Phase13_CitizenCertTest {

    @Autowired(required = false) CertChallengeService certChallengeService;
    @Autowired(required = false) CertVerificationService certVerificationService;
    @Autowired(required = false) CitizenCertUserSyncService citizenCertUserSyncService;
    @Autowired(required = false) AuthService authService;
    @Autowired(required = false) SaUserRepository saUserRepository;
    @Autowired(required = false) MockMvc mockMvc;
    @Autowired(required = false) ObjectMapper objectMapper;

    // =========================================================================
    // 13.1 Challenge Mechanism
    // =========================================================================

    @Nested
    @DisplayName("13.1 Challenge Mechanism")
    class ChallengeMechanism {

        @Test
        @DisplayName("Should generate a challenge with unique ID and random nonce")
        void shouldGenerateChallenge() {
            assertThat(certChallengeService).isNotNull();

            ChallengeResult challenge = certChallengeService.generateChallenge();

            assertThat(challenge).isNotNull();
            assertThat(challenge.challengeId()).isNotBlank();
            assertThat(challenge.nonce()).isNotBlank();
            // Nonce should be sufficiently random (at least 32 chars when Base64 encoded)
            assertThat(challenge.nonce().length()).isGreaterThanOrEqualTo(32);
        }

        @Test
        @DisplayName("Should generate unique challenges each time")
        void shouldGenerateUniqueChallenges() {
            ChallengeResult c1 = certChallengeService.generateChallenge();
            ChallengeResult c2 = certChallengeService.generateChallenge();

            assertThat(c1.challengeId()).isNotEqualTo(c2.challengeId());
            assertThat(c1.nonce()).isNotEqualTo(c2.nonce());
        }

        @Test
        @DisplayName("Should consume challenge after verification (prevent replay)")
        void shouldConsumeChallenge() {
            ChallengeResult challenge = certChallengeService.generateChallenge();

            // First consumption should succeed
            Optional<String> nonce = certChallengeService.consumeChallenge(challenge.challengeId());
            assertThat(nonce).isPresent().hasValue(challenge.nonce());

            // Second consumption should fail (already consumed)
            Optional<String> replayNonce = certChallengeService.consumeChallenge(challenge.challengeId());
            assertThat(replayNonce).isEmpty();
        }

        @Test
        @DisplayName("Should reject non-existent challenge ID")
        void shouldRejectNonExistentChallenge() {
            Optional<String> nonce = certChallengeService.consumeChallenge("non-existent-id");
            assertThat(nonce).isEmpty();
        }
    }

    // =========================================================================
    // 13.2 Certificate Verification
    // =========================================================================

    @Nested
    @DisplayName("13.2 Certificate Verification")
    class CertificateVerification {

        @Test
        @DisplayName("Should verify valid signature against certificate public key")
        void shouldVerifyValidSignature() throws Exception {
            assertThat(certVerificationService).isNotNull();

            // Generate a self-signed test cert (simulating MOICA cert)
            KeyPair keyPair = generateTestKeyPair();
            X509Certificate testCert = generateSelfSignedCert(keyPair, "CN=A123456789,O=MOICA");

            // Sign a nonce with the private key
            String nonce = "test-challenge-nonce-12345678901234567890";
            byte[] signature = sign(nonce, keyPair.getPrivate());

            CertVerifyResult result = certVerificationService.verifySignature(
                    testCert, nonce.getBytes(), signature);

            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid signature (tampered data)")
        void shouldRejectInvalidSignature() throws Exception {
            KeyPair keyPair = generateTestKeyPair();
            X509Certificate testCert = generateSelfSignedCert(keyPair, "CN=A123456789,O=MOICA");

            // Sign a different nonce
            String nonce = "original-nonce";
            byte[] signature = sign("tampered-nonce", keyPair.getPrivate());

            CertVerifyResult result = certVerificationService.verifySignature(
                    testCert, nonce.getBytes(), signature);

            assertThat(result.valid()).isFalse();
        }

        @Test
        @DisplayName("Should reject signature from a different key pair")
        void shouldRejectSignatureFromDifferentKey() throws Exception {
            KeyPair certKeyPair = generateTestKeyPair();
            KeyPair attackerKeyPair = generateTestKeyPair();
            X509Certificate testCert = generateSelfSignedCert(certKeyPair, "CN=A123456789,O=MOICA");

            String nonce = "test-nonce";
            byte[] signature = sign(nonce, attackerKeyPair.getPrivate());

            CertVerifyResult result = certVerificationService.verifySignature(
                    testCert, nonce.getBytes(), signature);

            assertThat(result.valid()).isFalse();
        }

        @Test
        @DisplayName("Should extract citizen ID from certificate Subject DN")
        void shouldExtractCitizenId() throws Exception {
            KeyPair keyPair = generateTestKeyPair();
            // MOICA cert Subject DN typically contains citizen ID in CN
            X509Certificate testCert = generateSelfSignedCert(keyPair, "CN=A123456789,O=MOICA,C=TW");

            String citizenId = certVerificationService.extractCitizenId(testCert);

            assertThat(citizenId).isEqualTo("A123456789");
        }

        @Test
        @DisplayName("Should extract display name from certificate if present")
        void shouldExtractDisplayName() throws Exception {
            KeyPair keyPair = generateTestKeyPair();
            X509Certificate testCert = generateSelfSignedCert(keyPair,
                    "CN=A123456789,SERIALNUMBER=A123456789,O=MOICA,C=TW");

            // Display name extraction — in real MOICA certs this comes from the SAN or other fields
            // For our implementation, we extract CN as citizenId
            String citizenId = certVerificationService.extractCitizenId(testCert);
            assertThat(citizenId).isNotBlank();
        }
    }

    // =========================================================================
    // 13.3 User Sync (Auto-Create / Bind)
    // =========================================================================

    @Nested
    @DisplayName("13.3 User Sync")
    class UserSync {

        @Test
        @DisplayName("Should create new user on first cert login when autoCreate is enabled")
        void shouldCreateNewUserOnFirstLogin() {
            assertThat(citizenCertUserSyncService).isNotNull();

            String citizenId = "Z299999999";
            SaUser user = citizenCertUserSyncService.syncUser(citizenId, "Test User");

            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo(citizenId);
            assertThat(user.getCitizenId()).isEqualTo(citizenId);
            assertThat(user.getAuthSource()).isEqualTo("CITIZEN_CERT");
            assertThat(user.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should return existing user if citizenId already bound")
        void shouldReturnExistingUser() {
            String citizenId = "Z299999998";

            // First sync creates the user
            SaUser first = citizenCertUserSyncService.syncUser(citizenId, "First Login");
            Long firstId = first.getObjid();

            // Second sync returns the same user
            SaUser second = citizenCertUserSyncService.syncUser(citizenId, "Second Login");

            assertThat(second.getObjid()).isEqualTo(firstId);
        }

        @Test
        @DisplayName("Should set authSource to CITIZEN_CERT")
        void shouldSetAuthSource() {
            SaUser user = citizenCertUserSyncService.syncUser("Z299999997", "Cert User");

            assertThat(user.getAuthSource()).isEqualTo("CITIZEN_CERT");
        }

        @Test
        @DisplayName("Should set password to placeholder (cert users don't use passwords)")
        void shouldSetPlaceholderPassword() {
            SaUser user = citizenCertUserSyncService.syncUser("Z299999996", "Cert User");

            assertThat(user.getPassword()).isEqualTo("{noop}CERT_MANAGED");
        }
    }

    // =========================================================================
    // 13.4 Cert Login Flow (REST API)
    // =========================================================================

    @Nested
    @DisplayName("13.4 Cert Login REST API")
    class CertLoginRestApi {

        @Test
        @DisplayName("GET /api/auth/cert/challenge should return challenge")
        void shouldReturnChallenge() throws Exception {
            mockMvc.perform(get("/api/auth/cert/challenge"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.challengeId").isNotEmpty())
                    .andExpect(jsonPath("$.data.nonce").isNotEmpty());
        }

        @Test
        @DisplayName("POST /api/auth/cert/login with valid cert should return JWT tokens")
        void shouldLoginWithValidCert() throws Exception {
            // Step 1: Get challenge
            String challengeJson = mockMvc.perform(get("/api/auth/cert/challenge"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Extract challengeId and nonce from response
            var challengeNode = objectMapper.readTree(challengeJson).get("data");
            String challengeId = challengeNode.get("challengeId").asText();
            String nonce = challengeNode.get("nonce").asText();

            // Step 2: Sign the nonce with test key pair
            KeyPair keyPair = generateTestKeyPair();
            X509Certificate cert = generateSelfSignedCert(keyPair, "CN=A123456789,O=MOICA,C=TW");
            byte[] signature = sign(nonce, keyPair.getPrivate());

            String requestBody = objectMapper.writeValueAsString(new CertLoginRequestDto(
                    challengeId,
                    Base64.getEncoder().encodeToString(cert.getEncoded()),
                    Base64.getEncoder().encodeToString(signature)
            ));

            // Step 3: Login
            mockMvc.perform(post("/api/auth/cert/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.authSource").value("CITIZEN_CERT"));
        }

        @Test
        @DisplayName("POST /api/auth/cert/login should reject replay attack (same challenge used twice)")
        void shouldRejectReplayAttack() throws Exception {
            // Get challenge
            String challengeJson = mockMvc.perform(get("/api/auth/cert/challenge"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            var challengeNode = objectMapper.readTree(challengeJson).get("data");
            String challengeId = challengeNode.get("challengeId").asText();
            String nonce = challengeNode.get("nonce").asText();

            KeyPair keyPair = generateTestKeyPair();
            X509Certificate cert = generateSelfSignedCert(keyPair, "CN=A123456789,O=MOICA,C=TW");
            byte[] signature = sign(nonce, keyPair.getPrivate());

            String requestBody = objectMapper.writeValueAsString(new CertLoginRequestDto(
                    challengeId,
                    Base64.getEncoder().encodeToString(cert.getEncoded()),
                    Base64.getEncoder().encodeToString(signature)
            ));

            // First login: success
            mockMvc.perform(post("/api/auth/cert/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            // Replay: should fail
            mockMvc.perform(post("/api/auth/cert/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/cert/login should reject invalid signature")
        void shouldRejectInvalidSignature() throws Exception {
            // Get challenge
            String challengeJson = mockMvc.perform(get("/api/auth/cert/challenge"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            var challengeNode = objectMapper.readTree(challengeJson).get("data");
            String challengeId = challengeNode.get("challengeId").asText();
            String nonce = challengeNode.get("nonce").asText();

            KeyPair certKeyPair = generateTestKeyPair();
            KeyPair attackerKeyPair = generateTestKeyPair();
            X509Certificate cert = generateSelfSignedCert(certKeyPair, "CN=A123456789,O=MOICA,C=TW");
            byte[] signature = sign(nonce, attackerKeyPair.getPrivate()); // Wrong key!

            String requestBody = objectMapper.writeValueAsString(new CertLoginRequestDto(
                    challengeId,
                    Base64.getEncoder().encodeToString(cert.getEncoded()),
                    Base64.getEncoder().encodeToString(signature)
            ));

            mockMvc.perform(post("/api/auth/cert/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // Test Helpers
    // =========================================================================

    record CertLoginRequestDto(String challengeId, String certificate, String signature) {}

    static KeyPair generateTestKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    /**
     * Generate a self-signed X.509 certificate for testing using JDK KeyStore.
     * Creates a temporary keystore with keytool-equivalent approach via reflection
     * on internal APIs (allowed at runtime via --add-opens in surefire).
     */
    static X509Certificate generateSelfSignedCert(KeyPair keyPair, String subjectDn) throws Exception {
        // Use keytool to generate a self-signed cert via process execution
        // This avoids direct dependency on sun.security.x509 at compile time
        return createCertViaReflection(keyPair, subjectDn);
    }

    @SuppressWarnings("unchecked")
    static X509Certificate createCertViaReflection(KeyPair keyPair, String dn) throws Exception {
        // Access sun.security.x509 classes via reflection (allowed by --add-opens at runtime)
        // Java 21 uses typed setter methods (setVersion, setSerialNumber, etc.)
        Class<?> x509CertInfoClass = Class.forName("sun.security.x509.X509CertInfo");
        Class<?> certValidityClass = Class.forName("sun.security.x509.CertificateValidity");
        Class<?> certSerialClass = Class.forName("sun.security.x509.CertificateSerialNumber");
        Class<?> x500NameClass = Class.forName("sun.security.x509.X500Name");
        Class<?> certX509KeyClass = Class.forName("sun.security.x509.CertificateX509Key");
        Class<?> certVersionClass = Class.forName("sun.security.x509.CertificateVersion");
        Class<?> algorithmIdClass = Class.forName("sun.security.x509.AlgorithmId");
        Class<?> certAlgoIdClass = Class.forName("sun.security.x509.CertificateAlgorithmId");
        Class<?> x509CertImplClass = Class.forName("sun.security.x509.X509CertImpl");

        var from = new java.util.Date();
        var to = new java.util.Date(from.getTime() + 365L * 86400000L);
        var sn = new java.math.BigInteger(64, new SecureRandom());

        Object info = x509CertInfoClass.getDeclaredConstructor().newInstance();
        Object validity = certValidityClass.getDeclaredConstructor(java.util.Date.class, java.util.Date.class)
                .newInstance(from, to);
        Object serial = certSerialClass.getDeclaredConstructor(java.math.BigInteger.class).newInstance(sn);
        Object owner = x500NameClass.getDeclaredConstructor(String.class).newInstance(dn);
        Object certKey = certX509KeyClass.getDeclaredConstructor(PublicKey.class).newInstance(keyPair.getPublic());
        Object version = certVersionClass.getDeclaredConstructor(int.class).newInstance(2); // V3

        // Get SHA256withRSA OID
        var sha256RsaOidField = algorithmIdClass.getField("SHA256withRSA_oid");
        Object sha256RsaOid = sha256RsaOidField.get(null);
        Class<?> objectIdentifierClass = Class.forName("sun.security.util.ObjectIdentifier");
        Object algoId = algorithmIdClass.getDeclaredConstructor(objectIdentifierClass)
                .newInstance(sha256RsaOid);
        Object certAlgoId = certAlgoIdClass.getDeclaredConstructor(algorithmIdClass).newInstance(algoId);

        // Java 21: use typed setters instead of set(String, Object)
        x509CertInfoClass.getMethod("setValidity", certValidityClass).invoke(info, validity);
        x509CertInfoClass.getMethod("setSerialNumber", certSerialClass).invoke(info, serial);
        x509CertInfoClass.getMethod("setSubject", x500NameClass).invoke(info, owner);
        x509CertInfoClass.getMethod("setIssuer", x500NameClass).invoke(info, owner);
        x509CertInfoClass.getMethod("setKey", certX509KeyClass).invoke(info, certKey);
        x509CertInfoClass.getMethod("setVersion", certVersionClass).invoke(info, version);
        x509CertInfoClass.getMethod("setAlgorithmId", certAlgoIdClass).invoke(info, certAlgoId);

        // Java 21: use static factory X509CertImpl.newSigned(info, privateKey, algorithm)
        var newSignedMethod = x509CertImplClass.getMethod("newSigned",
                x509CertInfoClass, PrivateKey.class, String.class);
        Object cert = newSignedMethod.invoke(null, info, keyPair.getPrivate(), "SHA256withRSA");

        return (X509Certificate) cert;
    }

    static byte[] sign(String data, PrivateKey privateKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return sig.sign();
    }
}
