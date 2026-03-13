package com.company.common.security.cert;

import com.company.common.security.cert.exception.MoicaExpiredException;
import com.company.common.security.cert.exception.MoicaNotYetValidException;
import com.company.common.security.cert.exception.MoicaRevocationException;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.Security;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for verifying MOICA (Ministry of the Interior Certification Authority)
 * citizen digital certificates.
 * <p>
 * Provides:
 * - Intermediate CA certificate chain validation
 * - CRL and OCSP revocation checking (with CRL caching)
 * - Certificate validity period checking
 * - Subject CN (cname) extraction
 * - Last 4 digits of ID number extraction from extension OID 2.16.886.1.100.1.1
 */
public class MoicaCertUtils {

    private static final Logger log = LoggerFactory.getLogger(MoicaCertUtils.class);
    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");

    /**
     * MOICA custom OID for Taiwan national ID number extension.
     * OID: 2.16.886.1.100.1.1 contains the full or partial ID number.
     */
    private static final String MOICA_IDNO_OID = "2.16.886.1.100.1.1";

    private static final int OCSP_CONNECT_TIMEOUT_MS = 5000;
    private static final int OCSP_READ_TIMEOUT_MS = 10000;
    private static final int CRL_CONNECT_TIMEOUT_MS = 5000;
    private static final int CRL_READ_TIMEOUT_MS = 15000;

    private final X509Certificate certificate;
    private final List<X509Certificate> intermediateCerts;
    private final boolean ocspEnabled;
    private final boolean crlEnabled;

    // In-memory CRL cache: URL -> CachedCrl
    private static final Map<String, CachedCrl> crlCache = new ConcurrentHashMap<>();

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * @param certificate       the end-entity citizen certificate to verify
     * @param intermediateCerts the list of MOICA intermediate CA certificates (e.g., MOICA2.cer, MOICA3.cer)
     * @param ocspEnabled       whether to perform OCSP revocation checking
     * @param crlEnabled        whether to perform CRL revocation checking
     */
    public MoicaCertUtils(X509Certificate certificate, List<X509Certificate> intermediateCerts,
                          boolean ocspEnabled, boolean crlEnabled) {
        this.certificate = certificate;
        this.intermediateCerts = intermediateCerts != null ? intermediateCerts : List.of();
        this.ocspEnabled = ocspEnabled;
        this.crlEnabled = crlEnabled;
    }

    /**
     * Convenience constructor using default revocation settings (both enabled).
     */
    public MoicaCertUtils(X509Certificate certificate, List<X509Certificate> intermediateCerts) {
        this(certificate, intermediateCerts, true, true);
    }

    /**
     * Validate that the certificate was issued by one of the trusted MOICA intermediate CAs.
     * Checks that the certificate's issuer matches one of the intermediate CA subjects
     * and that the certificate signature can be verified by the intermediate CA's public key.
     *
     * @return true if the certificate chain is valid
     */
    public boolean validIntermediateCert() {
        if (intermediateCerts.isEmpty()) {
            log.warn("No intermediate CA certificates configured, skipping chain validation");
            return true;
        }

        for (X509Certificate intermediateCert : intermediateCerts) {
            try {
                // Check issuer matches intermediate CA subject
                if (certificate.getIssuerX500Principal().equals(intermediateCert.getSubjectX500Principal())) {
                    // Verify certificate was signed by this intermediate CA
                    certificate.verify(intermediateCert.getPublicKey());
                    log.debug("Certificate chain validated against intermediate CA: {}",
                            intermediateCert.getSubjectX500Principal());
                    return true;
                }
            } catch (Exception e) {
                log.debug("Chain validation failed against CA {}: {}",
                        intermediateCert.getSubjectX500Principal(), e.getMessage());
            }
        }

        log.warn("Certificate issuer does not match any trusted MOICA intermediate CA. Issuer: {}",
                certificate.getIssuerX500Principal());
        return false;
    }

    /**
     * Check if the certificate has been revoked.
     * Strategy: try OCSP first (if enabled), fallback to CRL (if enabled).
     *
     * @return true if the certificate IS revoked; false if it is NOT revoked (good)
     */
    public boolean isRevoked() {
        // Try OCSP first
        if (ocspEnabled) {
            try {
                Boolean ocspResult = checkOcsp();
                if (ocspResult != null) {
                    return ocspResult;
                }
                log.debug("OCSP check inconclusive, falling back to CRL");
            } catch (Exception e) {
                log.warn("OCSP check failed, falling back to CRL: {}", e.getMessage());
            }
        }

        // Fallback to CRL
        if (crlEnabled) {
            try {
                return checkCrl();
            } catch (Exception e) {
                log.error("CRL check also failed: {}", e.getMessage());
                // If both checks fail, we should treat it as potentially revoked for security
                throw new MoicaRevocationException("Unable to verify certificate revocation status", e);
            }
        }

        // Both disabled
        log.info("Both OCSP and CRL checks are disabled, skipping revocation check");
        return false;
    }

    /**
     * Get the certificate serial number in hex format.
     */
    public String getSN() {
        return certificate.getSerialNumber().toString(16).toUpperCase();
    }

    /**
     * Extract the Subject CN (Common Name) from the certificate.
     * For MOICA citizen certificates, this typically contains the person's name.
     *
     * @return the CN value, or null if not found
     */
    public String getSubjectCName() {
        X500Principal principal = certificate.getSubjectX500Principal();
        String dn = principal.getName(X500Principal.RFC2253);
        Matcher matcher = CN_PATTERN.matcher(dn);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        log.warn("Could not extract CN from Subject DN: {}", dn);
        return null;
    }

    /**
     * Extract the last 4 digits of the national ID number from the certificate extension.
     * MOICA uses a custom extension with OID 2.16.886.1.100.1.1.
     * The extension value typically contains a UTF-8 string with the ID info.
     *
     * @return the last 4 characters of the ID number, or null if the extension is not present
     */
    public String getExtLast4IDNO() {
        try {
            byte[] extValue = certificate.getExtensionValue(MOICA_IDNO_OID);
            if (extValue == null) {
                log.debug("MOICA ID extension (OID: {}) not found in certificate", MOICA_IDNO_OID);
                return null;
            }

            // The extension value is wrapped in an OCTET STRING
            ASN1OctetString octetString = ASN1OctetString.getInstance(extValue);
            byte[] octets = octetString.getOctets();

            // Try to parse as ASN1 first
            try (ASN1InputStream asn1In = new ASN1InputStream(octets)) {
                ASN1Primitive primitive = asn1In.readObject();
                String idValue;
                if (primitive instanceof ASN1OctetString innerOctet) {
                    idValue = new String(innerOctet.getOctets(), java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    idValue = primitive.toString();
                }

                // Return last 4 characters
                if (idValue != null && idValue.length() >= 4) {
                    String last4 = idValue.substring(idValue.length() - 4);
                    log.debug("Extracted last4IDNO: {}", last4);
                    return last4;
                }
                log.warn("ID extension value too short: {}", idValue);
                return idValue;
            }
        } catch (Exception e) {
            log.error("Failed to extract last4IDNO from certificate extension: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validate that the certificate is within its validity period.
     *
     * @throws MoicaExpiredException if the certificate has expired
     * @throws MoicaNotYetValidException if the certificate is not yet valid
     */
    public void checkValidity() {
        try {
            certificate.checkValidity();
        } catch (CertificateExpiredException e) {
            throw new MoicaExpiredException("Certificate has expired: " + e.getMessage(), e);
        } catch (CertificateNotYetValidException e) {
            throw new MoicaNotYetValidException("Certificate is not yet valid: " + e.getMessage(), e);
        }
    }

    // ========== OCSP ==========

    /**
     * Check certificate revocation via OCSP.
     *
     * @return true if revoked, false if good, null if OCSP check was inconclusive
     */
    private Boolean checkOcsp() throws Exception {
        String ocspUrl = getOcspUrl();
        if (ocspUrl == null) {
            log.debug("No OCSP responder URL found in certificate");
            return null;
        }

        // Find the issuer certificate
        X509Certificate issuerCert = findIssuerCert();
        if (issuerCert == null) {
            log.warn("Cannot perform OCSP check: issuer certificate not found");
            return null;
        }

        log.debug("Performing OCSP check against: {}", ocspUrl);

        DigestCalculatorProvider digCalcProv = new JcaDigestCalculatorProviderBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build();

        CertificateID certId = new CertificateID(
                digCalcProv.get(CertificateID.HASH_SHA1),
                new X509CertificateHolder(issuerCert.getEncoded()),
                certificate.getSerialNumber());

        OCSPReqBuilder ocspReqBuilder = new OCSPReqBuilder();
        ocspReqBuilder.addRequest(certId);
        OCSPReq ocspReq = ocspReqBuilder.build();

        // Send OCSP request
        byte[] ocspReqBytes = ocspReq.getEncoded();
        HttpURLConnection conn = (HttpURLConnection) URI.create(ocspUrl).toURL().openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/ocsp-request");
            conn.setRequestProperty("Accept", "application/ocsp-response");
            conn.setDoOutput(true);
            conn.setConnectTimeout(OCSP_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(OCSP_READ_TIMEOUT_MS);

            conn.getOutputStream().write(ocspReqBytes);
            conn.getOutputStream().flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.warn("OCSP responder returned HTTP {}", conn.getResponseCode());
                return null;
            }

            byte[] respBytes = conn.getInputStream().readAllBytes();
            OCSPResp ocspResp = new OCSPResp(respBytes);

            if (ocspResp.getStatus() != OCSPResp.SUCCESSFUL) {
                log.warn("OCSP response status: {}", ocspResp.getStatus());
                return null;
            }

            BasicOCSPResp basicResp = (BasicOCSPResp) ocspResp.getResponseObject();
            for (SingleResp singleResp : basicResp.getResponses()) {
                if (singleResp.getCertID().equals(certId)) {
                    CertificateStatus status = singleResp.getCertStatus();
                    if (status == CertificateStatus.GOOD) {
                        log.debug("OCSP: certificate status is GOOD");
                        return false; // not revoked
                    } else {
                        log.warn("OCSP: certificate is REVOKED");
                        return true; // revoked
                    }
                }
            }

            return null; // no matching response
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Extract OCSP responder URL from the Authority Information Access extension.
     */
    private String getOcspUrl() {
        try {
            byte[] aiaExtValue = certificate.getExtensionValue(Extension.authorityInfoAccess.getId());
            if (aiaExtValue == null) {
                return null;
            }

            ASN1OctetString octetString = ASN1OctetString.getInstance(aiaExtValue);
            AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(
                    ASN1Sequence.getInstance(octetString.getOctets()));

            for (AccessDescription ad : aia.getAccessDescriptions()) {
                if (ad.getAccessMethod().equals(AccessDescription.id_ad_ocsp)) {
                    GeneralName gn = ad.getAccessLocation();
                    if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        return DERIA5String.getInstance(gn.getName()).getString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract OCSP URL: {}", e.getMessage());
        }
        return null;
    }

    // ========== CRL ==========

    /**
     * Check certificate revocation via CRL.
     *
     * @return true if revoked, false if not revoked
     */
    private boolean checkCrl() throws Exception {
        List<String> crlUrls = getCrlDistributionPoints();
        if (crlUrls.isEmpty()) {
            log.warn("No CRL distribution points found in certificate");
            return false;
        }

        for (String crlUrl : crlUrls) {
            try {
                X509CRL crl = downloadCrl(crlUrl);
                if (crl != null && crl.isRevoked(certificate)) {
                    log.warn("Certificate is REVOKED according to CRL: {}", crlUrl);
                    return true;
                }
                log.debug("Certificate is not revoked according to CRL: {}", crlUrl);
                return false;
            } catch (Exception e) {
                log.warn("Failed to check CRL at {}: {}", crlUrl, e.getMessage());
            }
        }

        throw new MoicaRevocationException("Failed to check any CRL distribution point");
    }

    /**
     * Extract CRL Distribution Point URLs from the certificate.
     */
    private List<String> getCrlDistributionPoints() {
        List<String> urls = new ArrayList<>();
        try {
            byte[] crlDpExtValue = certificate.getExtensionValue(Extension.cRLDistributionPoints.getId());
            if (crlDpExtValue == null) {
                return urls;
            }

            ASN1OctetString octetString = ASN1OctetString.getInstance(crlDpExtValue);
            CRLDistPoint distPoint = CRLDistPoint.getInstance(
                    ASN1Sequence.getInstance(octetString.getOctets()));

            for (DistributionPoint dp : distPoint.getDistributionPoints()) {
                DistributionPointName dpName = dp.getDistributionPoint();
                if (dpName != null && dpName.getType() == DistributionPointName.FULL_NAME) {
                    GeneralNames gns = GeneralNames.getInstance(dpName.getName());
                    for (GeneralName gn : gns.getNames()) {
                        if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                            urls.add(DERIA5String.getInstance(gn.getName()).getString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract CRL distribution points: {}", e.getMessage());
        }
        return urls;
    }

    /**
     * Download and parse a CRL, with in-memory caching.
     */
    private X509CRL downloadCrl(String crlUrl) throws Exception {
        // Check cache
        CachedCrl cached = crlCache.get(crlUrl);
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached CRL for: {}", crlUrl);
            return cached.crl();
        }

        log.debug("Downloading CRL from: {}", crlUrl);
        HttpURLConnection conn = (HttpURLConnection) URI.create(crlUrl).toURL().openConnection();
        try {
            conn.setConnectTimeout(CRL_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(CRL_READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("CRL download failed with HTTP " + conn.getResponseCode());
            }

            try (InputStream is = conn.getInputStream()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509CRL crl = (X509CRL) cf.generateCRL(is);

                // Cache with default 1-hour TTL
                crlCache.put(crlUrl, new CachedCrl(crl, System.currentTimeMillis()));
                log.debug("CRL downloaded and cached: {} (next update: {})", crlUrl, crl.getNextUpdate());
                return crl;
            }
        } finally {
            conn.disconnect();
        }
    }

    private X509Certificate findIssuerCert() {
        for (X509Certificate ca : intermediateCerts) {
            if (certificate.getIssuerX500Principal().equals(ca.getSubjectX500Principal())) {
                return ca;
            }
        }
        return null;
    }

    /**
     * Clear the CRL cache (useful for testing or forced refresh).
     */
    public static void clearCrlCache() {
        crlCache.clear();
    }

    /**
     * Set the CRL cache TTL in hours. The default is 1 hour.
     */
    public static void setCrlCacheTtlHours(int hours) {
        CachedCrl.cacheTtlMs = hours * 3600L * 1000L;
    }

    // ========== Inner classes ==========

    private record CachedCrl(X509CRL crl, long cachedAtMs) {
        static long cacheTtlMs = 3600_000L; // default 1 hour

        boolean isExpired() {
            return System.currentTimeMillis() - cachedAtMs > cacheTtlMs;
        }
    }
}
