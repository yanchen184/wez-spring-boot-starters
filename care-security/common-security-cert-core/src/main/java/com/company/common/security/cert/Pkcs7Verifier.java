package com.company.common.security.cert;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;

/**
 * Shared PKCS#7 (CMS SignedData) verification utility.
 * <p>
 * Used by all certificate authentication modules (MOICA, GCA, MOEACA, XCA)
 * to verify digital signatures and extract signer certificates.
 */
public class Pkcs7Verifier {

    private static final Logger log = LoggerFactory.getLogger(Pkcs7Verifier.class);

    private final CMSSignedData cmsSignedData;
    private X509Certificate cachedCert;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * @param base64Data Base64-encoded PKCS#7 SignedData (DER format)
     * @param signedContent the original content that was signed (e.g., loginToken)
     */
    public Pkcs7Verifier(String base64Data, String signedContent) {
        try {
            byte[] pkcs7Bytes = Base64.getDecoder().decode(base64Data);
            CMSProcessableByteArray content = new CMSProcessableByteArray(
                    signedContent.getBytes(StandardCharsets.UTF_8));
            this.cmsSignedData = new CMSSignedData(content, pkcs7Bytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse PKCS#7 data: " + e.getMessage(), e);
        }
    }

    /**
     * Verify the PKCS#7 signature.
     *
     * @return true if the signature is valid
     */
    public boolean verify() {
        try {
            SignerInformationStore signerStore = cmsSignedData.getSignerInfos();
            Collection<SignerInformation> signers = signerStore.getSigners();

            if (signers.isEmpty()) {
                log.warn("No signers found in PKCS#7 data");
                return false;
            }

            X509Certificate cert = extractCertificate();
            if (cert == null) {
                log.warn("No certificate found in PKCS#7 data");
                return false;
            }

            X509CertificateHolder certHolder = new X509CertificateHolder(cert.getEncoded());

            for (SignerInformation signer : signers) {
                boolean verified = signer.verify(
                        new JcaSimpleSignerInfoVerifierBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .build(certHolder));
                if (verified) {
                    log.debug("PKCS#7 signature verification succeeded");
                    return true;
                }
            }

            log.warn("PKCS#7 signature verification failed for all signers");
            return false;
        } catch (Exception e) {
            log.error("PKCS#7 signature verification error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract the X509Certificate from the PKCS#7 envelope.
     *
     * @return the signer's certificate, or null if not found
     */
    @SuppressWarnings("unchecked")
    public X509Certificate extractCertificate() {
        if (cachedCert != null) {
            return cachedCert;
        }
        try {
            Store<X509CertificateHolder> certStore = cmsSignedData.getCertificates();
            SignerInformationStore signerStore = cmsSignedData.getSignerInfos();

            for (SignerInformation signer : signerStore.getSigners()) {
                Collection<X509CertificateHolder> matches = certStore.getMatches(signer.getSID());
                if (!matches.isEmpty()) {
                    cachedCert = new JcaX509CertificateConverter()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .getCertificate(matches.iterator().next());
                    return cachedCert;
                }
            }

            // Fallback: first certificate in store
            Collection<X509CertificateHolder> allCerts = certStore.getMatches(null);
            if (!allCerts.isEmpty()) {
                cachedCert = new JcaX509CertificateConverter()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .getCertificate(allCerts.iterator().next());
                return cachedCert;
            }

            log.warn("No certificates found in PKCS#7 envelope");
            return null;
        } catch (Exception e) {
            log.error("Failed to extract certificate from PKCS#7: {}", e.getMessage(), e);
            return null;
        }
    }
}
