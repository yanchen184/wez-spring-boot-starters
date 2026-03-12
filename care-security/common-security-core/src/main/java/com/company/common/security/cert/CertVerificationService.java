package com.company.common.security.cert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies X.509 certificate signatures and extracts citizen identity information.
 * In production, this would also validate the certificate chain against MOICA Root CA
 * and check CRL/OCSP revocation status.
 */
public class CertVerificationService {

    private static final Logger log = LoggerFactory.getLogger(CertVerificationService.class);
    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");

    /**
     * Verify that the signature was produced by the private key corresponding to the certificate's public key.
     */
    public CertVerifyResult verifySignature(X509Certificate certificate, byte[] data, byte[] signatureBytes) {
        try {
            // Use SHA256withRSA consistently for challenge-response signature verification
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(certificate.getPublicKey());
            sig.update(data);
            boolean valid = sig.verify(signatureBytes);

            if (valid) {
                log.debug("Signature verification succeeded for cert: {}", certificate.getSubjectX500Principal());
            } else {
                log.warn("Signature verification failed for cert: {}", certificate.getSubjectX500Principal());
            }

            return new CertVerifyResult(valid);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return new CertVerifyResult(false);
        }
    }

    /**
     * Extract the citizen ID from the certificate's Subject DN.
     * MOICA certificates typically store the citizen ID (身分證字號) in the CN field.
     *
     * Example Subject DN: "CN=A123456789,O=MOICA,C=TW"
     * Returns: "A123456789"
     */
    public String extractCitizenId(X509Certificate certificate) {
        X500Principal principal = certificate.getSubjectX500Principal();
        String dn = principal.getName(X500Principal.RFC2253);
        Matcher matcher = CN_PATTERN.matcher(dn);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        log.warn("Could not extract citizen ID from Subject DN: {}", dn);
        return null;
    }

    /**
     * Validate that the certificate is currently valid (not expired, not yet valid).
     */
    public boolean isCertificateValid(X509Certificate certificate) {
        try {
            certificate.checkValidity();
            return true;
        } catch (Exception e) {
            log.warn("Certificate validity check failed: {}", e.getMessage());
            return false;
        }
    }

    public record CertVerifyResult(boolean valid) {}
}
