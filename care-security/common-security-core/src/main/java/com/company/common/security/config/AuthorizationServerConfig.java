package com.company.common.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import com.company.common.security.autoconfigure.CareSecurityProperties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    public RegisteredClientRepository registeredClientRepository(CareSecurityProperties properties) {
        CareSecurityProperties.OAuth2Client oauth2 = properties.getOauth2Client();
        Objects.requireNonNull(oauth2.getClientSecret(),
                "care.security.oauth2-client.client-secret must be configured");

        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(oauth2.getClientId())
                .clientSecret(oauth2.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri(oauth2.getRedirectUri())
                .scope("read")
                .scope("write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(webClient);
    }

    /**
     * Load or create RSA key for JWT signing.
     * <p>
     * If {@code keystorePath} is set:
     * <ul>
     *   <li>File exists → load the persisted JWK JSON and reuse the same key</li>
     *   <li>File missing → generate a new RSA key pair, save as JWK JSON, reuse on next restart</li>
     * </ul>
     * If {@code keystorePath} is null/blank → in-memory random key (dev fallback, lost on restart).
     */
    public JWKSource<SecurityContext> jwkSource(String keystorePath) {
        RSAKey rsaKey;
        if (keystorePath == null || keystorePath.isBlank()) {
            rsaKey = generateRsaKey();
            log.warn("No jwt.keystore-path configured — using in-memory RSA key (tokens will not survive restart)");
        } else {
            rsaKey = loadOrCreate(resolveFilePath(keystorePath));
        }
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private RSAKey loadOrCreate(Path path) {
        if (Files.exists(path)) {
            return loadFromFile(path);
        }
        log.info("JWK file not found at {} — generating new RSA key pair", path);
        return generateAndSave(path);
    }

    private RSAKey loadFromFile(Path path) {
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JWKSet jwkSet = JWKSet.parse(json);
            RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0);
            log.info("Loaded existing RSA key from: {}", path);
            return rsaKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load JWK from: " + path, ex);
        }
    }

    private RSAKey generateAndSave(Path path) {
        try {
            RSAKey rsaKey = generateRsaKey("care-jwt");
            JWKSet jwkSet = new JWKSet(rsaKey);
            String json = jwkSet.toString(false);

            Files.createDirectories(path.getParent());
            Files.writeString(path, json, StandardCharsets.UTF_8);
            try {
                Files.setPosixFilePermissions(path, Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException ignored) {
                // Windows — no POSIX permissions
            }
            log.info("Generated and saved RSA key to: {}", path);
            return rsaKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate and save JWK to: " + path, ex);
        }
    }

    private static Path resolveFilePath(String keystorePath) {
        if (keystorePath.startsWith("file:")) {
            return Path.of(keystorePath.substring(5));
        }
        return Path.of(keystorePath);
    }

    private static RSAKey generateRsaKey() {
        return generateRsaKey(UUID.randomUUID().toString());
    }

    private static RSAKey generateRsaKey(String keyId) {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair keyPair = gen.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(keyId)
                    .build();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(
            com.company.common.security.security.JwtTokenCustomizer jwtTokenCustomizer) {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                jwtTokenCustomizer.customize(context);
            }
        };
    }
}
