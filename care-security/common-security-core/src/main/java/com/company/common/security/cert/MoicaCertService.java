package com.company.common.security.cert;

import com.company.common.security.cert.exception.MoicaLoginException;
import com.company.common.security.cert.exception.MoicaRevocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that loads MOICA intermediate CA certificates and provides a factory method
 * to create {@link MoicaCertUtils} instances for verifying citizen certificates.
 */
public class MoicaCertService {

    private static final Logger log = LoggerFactory.getLogger(MoicaCertService.class);

    private final List<X509Certificate> intermediateCerts;
    private final List<String> localCrlPaths;
    private final boolean ocspEnabled;
    private final boolean crlEnabled;

    public MoicaCertService(ResourceLoader resourceLoader, List<String> intermediateCertPaths,
                            List<String> localCrlPaths,
                            boolean ocspEnabled, boolean crlEnabled, int crlCacheTtlHours) {
        this.ocspEnabled = ocspEnabled;
        this.crlEnabled = crlEnabled;
        this.intermediateCerts = loadCertificates(resourceLoader, intermediateCertPaths);
        this.localCrlPaths = localCrlPaths != null ? localCrlPaths : List.of();
        MoicaCertUtils.setCrlCacheTtlHours(crlCacheTtlHours);
        log.info("MoicaCertService initialized: {} intermediate CAs, {} local CRL paths, OCSP={}, CRL={}",
                intermediateCerts.size(), this.localCrlPaths.size(), ocspEnabled, crlEnabled);
    }

    /**
     * Create a MoicaCertUtils instance for the given certificate.
     * Local CRL paths are passed as-is; MoicaCertUtils will read and cache them with TTL.
     */
    public MoicaCertUtils createVerifier(X509Certificate certificate) {
        return new MoicaCertUtils(certificate, intermediateCerts, localCrlPaths, ocspEnabled, crlEnabled);
    }

    /**
     * Full verification of a citizen certificate:
     * 1. Validity period check
     * 2. Intermediate CA chain validation
     * 3. Revocation check (OCSP/CRL)
     *
     * @param certificate the citizen certificate to verify
     * @throws MoicaLoginException if any verification step fails
     */
    public void fullVerify(X509Certificate certificate) {
        MoicaCertUtils verifier = createVerifier(certificate);

        // 1. Validity period
        verifier.checkValidity();

        // 2. Intermediate CA chain
        if (!verifier.validIntermediateCert()) {
            throw new MoicaLoginException("Certificate chain validation failed: "
                    + "issuer is not a trusted MOICA CA");
        }

        // 3. Revocation
        if (verifier.isRevoked()) {
            throw new MoicaRevocationException("Certificate has been revoked");
        }

        log.debug("Certificate fully verified: SN={}, CN={}",
                verifier.getSN(), verifier.getSubjectCName());
    }

    private List<X509Certificate> loadCertificates(ResourceLoader resourceLoader, List<String> paths) {
        List<X509Certificate> certs = new ArrayList<>();
        if (paths == null || paths.isEmpty()) {
            log.warn("No intermediate CA certificate paths configured");
            return certs;
        }

        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create CertificateFactory", e);
        }

        for (String path : paths) {
            try {
                Resource resource = resourceLoader.getResource(path);
                try (InputStream is = resource.getInputStream()) {
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
                    certs.add(cert);
                    log.info("Loaded intermediate CA: {} (valid until: {})",
                            cert.getSubjectX500Principal(), cert.getNotAfter());
                }
            } catch (Exception e) {
                log.error("Failed to load intermediate CA certificate from path: {}", path, e);
            }
        }
        return certs;
    }

}
