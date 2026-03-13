package com.company.common.security.cert;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
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
 * Utility for parsing and verifying PKCS#7 (CMS) signed data from MOICA citizen certificate.
 * <p>
 * The PKCS#7 envelope contains:
 * - The signer's X509 certificate
 * - The digital signature
 * - Optionally, the CardSN in unauthenticated attributes or a custom OID
 * <p>
 * The signed content is the loginToken provided by the server.
 */
public class Pkcs7Utils {

    private static final Logger log = LoggerFactory.getLogger(Pkcs7Utils.class);

    // MOICA CardSN OID (commonly used in Taiwan government PKI)
    private static final ASN1ObjectIdentifier CARD_SN_OID =
            new ASN1ObjectIdentifier("1.2.840.113549.1.9.25.3");

    private final CMSSignedData cmsSignedData;
    private final String loginToken;
    private X509Certificate cachedCert;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * @param base64Data Base64-encoded PKCS#7 SignedData (DER format)
     * @param loginToken the original login token that was signed by the client
     */
    public Pkcs7Utils(String base64Data, String loginToken) {
        try {
            byte[] pkcs7Bytes = Base64.getDecoder().decode(base64Data);
            // Create CMSSignedData with the expected content (loginToken)
            CMSProcessableByteArray content = new CMSProcessableByteArray(
                    loginToken.getBytes(StandardCharsets.UTF_8));
            this.cmsSignedData = new CMSSignedData(content, pkcs7Bytes);
            this.loginToken = loginToken;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse PKCS#7 data: " + e.getMessage(), e);
        }
    }

    /**
     * Verify the PKCS#7 signature. Confirms that the signed content matches the loginToken
     * and that the signature is valid against the embedded certificate.
     *
     * @return true if the signature is valid
     */
    public boolean valid() {
        try {
            SignerInformationStore signerStore = cmsSignedData.getSignerInfos();
            Collection<SignerInformation> signers = signerStore.getSigners();

            if (signers.isEmpty()) {
                log.warn("No signers found in PKCS#7 data");
                return false;
            }

            X509Certificate cert = getCert();
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
     * @return the signer's X509 certificate, or null if not found
     */
    @SuppressWarnings("unchecked")
    public X509Certificate getCert() {
        if (cachedCert != null) {
            return cachedCert;
        }
        try {
            Store<X509CertificateHolder> certStore = cmsSignedData.getCertificates();
            SignerInformationStore signerStore = cmsSignedData.getSignerInfos();

            for (SignerInformation signer : signerStore.getSigners()) {
                Collection<X509CertificateHolder> matches = certStore.getMatches(signer.getSID());
                if (!matches.isEmpty()) {
                    X509CertificateHolder holder = matches.iterator().next();
                    cachedCert = new JcaX509CertificateConverter()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .getCertificate(holder);
                    return cachedCert;
                }
            }

            // Fallback: take the first certificate in the store
            Collection<X509CertificateHolder> allCerts = certStore.getMatches(null);
            if (!allCerts.isEmpty()) {
                X509CertificateHolder holder = allCerts.iterator().next();
                cachedCert = new JcaX509CertificateConverter()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .getCertificate(holder);
                return cachedCert;
            }

            log.warn("No certificates found in PKCS#7 envelope");
            return null;
        } catch (Exception e) {
            log.error("Failed to extract certificate from PKCS#7: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract the CardSN (smart card serial number) from the PKCS#7 envelope.
     * The CardSN may be in:
     * 1. Unauthenticated attributes of the SignerInfo
     * 2. Authenticated attributes with a custom MOICA OID
     * 3. The certificate's serial number as fallback
     *
     * @return the card serial number, or the certificate serial number as fallback
     */
    public String getCardSN() {
        try {
            SignerInformationStore signerStore = cmsSignedData.getSignerInfos();
            for (SignerInformation signer : signerStore.getSigners()) {
                // Try unauthenticated attributes first
                String cardSn = extractCardSnFromAttributes(signer.getUnsignedAttributes());
                if (cardSn != null) {
                    return cardSn;
                }

                // Try authenticated attributes
                cardSn = extractCardSnFromAttributes(signer.getSignedAttributes());
                if (cardSn != null) {
                    return cardSn;
                }
            }

            // Fallback: use certificate serial number
            X509Certificate cert = getCert();
            if (cert != null) {
                String certSn = cert.getSerialNumber().toString(16).toUpperCase();
                log.debug("Using certificate serial number as CardSN fallback: {}", certSn);
                return certSn;
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to extract CardSN from PKCS#7: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractCardSnFromAttributes(AttributeTable attributes) {
        if (attributes == null) {
            return null;
        }
        Attribute attr = attributes.get(CARD_SN_OID);
        if (attr != null) {
            ASN1Set values = attr.getAttrValues();
            if (values.size() > 0) {
                String cardSn = values.getObjectAt(0).toString();
                log.debug("Found CardSN in PKCS#7 attributes: {}", cardSn);
                return cardSn;
            }
        }
        return null;
    }
}
