package com.company.common.security.cert;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
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
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
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

/**
 * Shared certificate chain validation and revocation checking.
 * <p>
 * Supports:
 * <ul>
 *   <li>Intermediate CA chain validation</li>
 *   <li>OCSP revocation checking</li>
 *   <li>CRL revocation checking (local file + network download, with TTL cache)</li>
 *   <li>Certificate validity period checking</li>
 * </ul>
 */
public class CertVerifier {

    private static final Logger log = LoggerFactory.getLogger(CertVerifier.class);

    private static final int OCSP_TIMEOUT_MS = 5000;
    private static final int CRL_TIMEOUT_MS = 15000;
    private static final Map<String, CachedCrl> crlCache = new ConcurrentHashMap<>();
    private static long crlCacheTtlMs = 3600_000L; // 1 hour

    private final X509Certificate certificate;
    private final List<X509Certificate> intermediateCerts;
    private final List<String> localCrlPaths;
    private final boolean ocspEnabled;
    private final boolean crlEnabled;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public CertVerifier(X509Certificate certificate, List<X509Certificate> intermediateCerts,
                        List<String> localCrlPaths, boolean ocspEnabled, boolean crlEnabled) {
        this.certificate = certificate;
        this.intermediateCerts = intermediateCerts != null ? intermediateCerts : List.of();
        this.localCrlPaths = localCrlPaths != null ? localCrlPaths : List.of();
        this.ocspEnabled = ocspEnabled;
        this.crlEnabled = crlEnabled;
    }

    public CertVerifier(X509Certificate certificate, List<X509Certificate> intermediateCerts) {
        this(certificate, intermediateCerts, List.of(), true, true);
    }

    /**
     * Validate the certificate was issued by a trusted intermediate CA.
     */
    public boolean validateChain() {
        if (intermediateCerts.isEmpty()) {
            log.warn("No intermediate CA certificates configured, skipping chain validation");
            return true;
        }
        for (X509Certificate ca : intermediateCerts) {
            try {
                if (certificate.getIssuerX500Principal().equals(ca.getSubjectX500Principal())) {
                    certificate.verify(ca.getPublicKey());
                    log.debug("Certificate chain validated against CA: {}", ca.getSubjectX500Principal());
                    return true;
                }
            } catch (Exception e) {
                log.debug("Chain validation failed against CA {}: {}", ca.getSubjectX500Principal(), e.getMessage());
            }
        }
        log.warn("Certificate issuer does not match any trusted intermediate CA. Issuer: {}",
                certificate.getIssuerX500Principal());
        return false;
    }

    /**
     * Check certificate validity period.
     *
     * @throws CertificateExpiredException if expired
     * @throws CertificateNotYetValidException if not yet valid
     */
    public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
        certificate.checkValidity();
    }

    /**
     * Check if the certificate has been revoked (OCSP first, then CRL fallback).
     *
     * @return true if revoked
     */
    public boolean isRevoked() {
        if (ocspEnabled) {
            try {
                Boolean result = checkOcsp();
                if (result != null) {
                    return result;
                }
                log.debug("OCSP inconclusive, falling back to CRL");
            } catch (Exception e) {
                log.warn("OCSP check failed: {}", e.getMessage());
            }
        }
        if (crlEnabled) {
            return checkCrl();
        }
        log.info("Both OCSP and CRL checks disabled");
        return false;
    }

    /**
     * Full verification: validity + chain + revocation.
     *
     * @throws CertVerificationException if any check fails
     */
    public void fullVerify() {
        try {
            checkValidity();
        } catch (CertificateExpiredException e) {
            throw new CertVerificationException("憑證已過期", e);
        } catch (CertificateNotYetValidException e) {
            throw new CertVerificationException("憑證尚未生效", e);
        }
        if (!validateChain()) {
            throw new CertVerificationException("憑證鏈驗證失敗：非受信任的中繼 CA 簽發");
        }
        if (isRevoked()) {
            throw new CertVerificationException("憑證已被撤銷");
        }
        log.debug("Certificate fully verified: SN={}", CertExtensionUtils.getSerialNumberHex(certificate));
    }

    // --- OCSP ---

    private Boolean checkOcsp() throws Exception {
        String ocspUrl = getOcspUrl();
        if (ocspUrl == null) {
            return null;
        }

        X509Certificate issuerCert = findIssuerCert();
        if (issuerCert == null) {
            return null;
        }

        log.debug("OCSP check against: {}", ocspUrl);

        CertificateID certId = new CertificateID(
                new JcaDigestCalculatorProviderBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build().get(CertificateID.HASH_SHA1),
                new X509CertificateHolder(issuerCert.getEncoded()),
                certificate.getSerialNumber());

        OCSPReq ocspReq = new OCSPReqBuilder().addRequest(certId).build();

        HttpURLConnection conn = (HttpURLConnection) URI.create(ocspUrl).toURL().openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/ocsp-request");
            conn.setDoOutput(true);
            conn.setConnectTimeout(OCSP_TIMEOUT_MS);
            conn.setReadTimeout(OCSP_TIMEOUT_MS);
            conn.getOutputStream().write(ocspReq.getEncoded());

            if (conn.getResponseCode() != 200) {
                return null;
            }

            OCSPResp resp = new OCSPResp(conn.getInputStream().readAllBytes());
            if (resp.getStatus() != OCSPResp.SUCCESSFUL) {
                return null;
            }

            BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();
            for (SingleResp single : basic.getResponses()) {
                if (single.getCertID().equals(certId)) {
                    return single.getCertStatus() != CertificateStatus.GOOD;
                }
            }
            return null;
        } finally {
            conn.disconnect();
        }
    }

    private String getOcspUrl() {
        try {
            byte[] aiaExt = certificate.getExtensionValue(org.bouncycastle.asn1.x509.Extension.authorityInfoAccess.getId());
            if (aiaExt == null) {
                return null;
            }
            AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(
                    ASN1Sequence.getInstance(ASN1OctetString.getInstance(aiaExt).getOctets()));
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

    // --- CRL ---

    private boolean checkCrl() {
        for (String path : localCrlPaths) {
            try {
                X509CRL crl = loadCrl(path);
                if (crl.getIssuerX500Principal().equals(certificate.getIssuerX500Principal())) {
                    return crl.isRevoked(certificate);
                }
            } catch (Exception e) {
                log.warn("Failed to read local CRL {}: {}", path, e.getMessage());
            }
        }

        for (String url : getCrlDistributionPoints()) {
            try {
                X509CRL crl = loadCrl(url);
                if (crl != null) {
                    return crl.isRevoked(certificate);
                }
            } catch (Exception e) {
                log.warn("Failed to check CRL {}: {}", url, e.getMessage());
            }
        }

        log.warn("No CRL could be checked");
        return false;
    }

    private X509CRL loadCrl(String pathOrUrl) throws Exception {
        CachedCrl cached = crlCache.get(pathOrUrl);
        if (cached != null && !cached.isExpired()) {
            return cached.crl;
        }

        InputStream is;
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            HttpURLConnection conn = (HttpURLConnection) URI.create(pathOrUrl).toURL().openConnection();
            conn.setConnectTimeout(CRL_TIMEOUT_MS);
            conn.setReadTimeout(CRL_TIMEOUT_MS);
            is = conn.getInputStream();
        } else if (pathOrUrl.startsWith("classpath:")) {
            is = getClass().getClassLoader().getResourceAsStream(pathOrUrl.substring("classpath:".length()));
            if (is == null) {
                throw new IOException("Classpath resource not found: " + pathOrUrl);
            }
        } else {
            String filePath = pathOrUrl.startsWith("file:") ? pathOrUrl.substring("file:".length()) : pathOrUrl;
            is = new FileInputStream(filePath);
        }

        try (is) {
            X509CRL crl = (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(is);
            crlCache.put(pathOrUrl, new CachedCrl(crl, System.currentTimeMillis()));
            return crl;
        }
    }

    private List<String> getCrlDistributionPoints() {
        List<String> urls = new ArrayList<>();
        try {
            byte[] ext = certificate.getExtensionValue(org.bouncycastle.asn1.x509.Extension.cRLDistributionPoints.getId());
            if (ext == null) {
                return urls;
            }
            CRLDistPoint distPoint = CRLDistPoint.getInstance(
                    ASN1Sequence.getInstance(ASN1OctetString.getInstance(ext).getOctets()));
            for (DistributionPoint dp : distPoint.getDistributionPoints()) {
                DistributionPointName dpName = dp.getDistributionPoint();
                if (dpName != null && dpName.getType() == DistributionPointName.FULL_NAME) {
                    for (GeneralName gn : GeneralNames.getInstance(dpName.getName()).getNames()) {
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

    private X509Certificate findIssuerCert() {
        for (X509Certificate ca : intermediateCerts) {
            if (certificate.getIssuerX500Principal().equals(ca.getSubjectX500Principal())) {
                return ca;
            }
        }
        return null;
    }

    public static void setCrlCacheTtlHours(int hours) { crlCacheTtlMs = hours * 3600_000L; }
    public static void clearCrlCache() { crlCache.clear(); }

    private record CachedCrl(X509CRL crl, long cachedAtMs) {
        boolean isExpired() { return System.currentTimeMillis() - cachedAtMs > crlCacheTtlMs; }
    }
}
