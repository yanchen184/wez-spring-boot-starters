package com.company.common.security.cert;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utilities for extracting data from X.509 certificate extensions.
 * Used by all Taiwan government certificate providers.
 */
public final class CertExtensionUtils {

    private static final Logger log = LoggerFactory.getLogger(CertExtensionUtils.class);
    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");

    private CertExtensionUtils() {}

    /**
     * Extract the Subject CN (Common Name) from the certificate.
     */
    public static String getSubjectCName(X509Certificate certificate) {
        String dn = certificate.getSubjectX500Principal().getName(javax.security.auth.x500.X500Principal.RFC2253);
        Matcher matcher = CN_PATTERN.matcher(dn);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        log.warn("Could not extract CN from Subject DN: {}", dn);
        return null;
    }

    /**
     * Get the certificate serial number in uppercase hex format.
     */
    public static String getSerialNumberHex(X509Certificate certificate) {
        return certificate.getSerialNumber().toString(16).toUpperCase();
    }

    /**
     * Extract a value from the Subject Directory Attributes extension (OID 2.5.29.9)
     * by matching a specific attribute OID.
     * <p>
     * Taiwan government certificates store identity info in this extension:
     * <ul>
     *   <li>2.16.886.1.100.2.51 — MOICA last4IDNO</li>
     *   <li>2.16.886.1.100.2.101 — MOEACA business ID (統一編號)</li>
     *   <li>2.16.886.1.100.2.102 — GCA/XCA organization OID</li>
     * </ul>
     *
     * @param certificate  the X.509 certificate
     * @param attributeOid the OID to look for within the Subject Directory Attributes
     * @return the extracted value, or null if not found
     */
    public static String extractSubjectDirectoryAttribute(X509Certificate certificate, String attributeOid) {
        try {
            // OID 2.5.29.9 = Subject Directory Attributes
            byte[] extValue = certificate.getExtensionValue("2.5.29.9");
            if (extValue == null) {
                return extractFromCustomExtension(certificate, attributeOid);
            }

            ASN1OctetString octetString = ASN1OctetString.getInstance(extValue);
            try (ASN1InputStream asn1In = new ASN1InputStream(octetString.getOctets())) {
                ASN1Primitive primitive = asn1In.readObject();
                if (primitive instanceof ASN1Sequence seq) {
                    return findAttributeInSequence(seq, attributeOid);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract attribute {} from certificate: {}", attributeOid, e.getMessage());
        }
        return null;
    }

    /**
     * Extract a value from a custom extension OID directly.
     * Used as fallback when the value is stored in a dedicated extension rather than
     * Subject Directory Attributes.
     */
    public static String extractFromCustomExtension(X509Certificate certificate, String oid) {
        try {
            byte[] extValue = certificate.getExtensionValue(oid);
            if (extValue == null) {
                return null;
            }

            ASN1OctetString outer = ASN1OctetString.getInstance(extValue);
            try (ASN1InputStream asn1In = new ASN1InputStream(outer.getOctets())) {
                ASN1Primitive primitive = asn1In.readObject();
                if (primitive instanceof ASN1OctetString innerOctet) {
                    return new String(innerOctet.getOctets(), StandardCharsets.UTF_8);
                }
                return primitive.toString();
            }
        } catch (Exception e) {
            log.debug("Failed to extract custom extension {}: {}", oid, e.getMessage());
            return null;
        }
    }

    private static String findAttributeInSequence(ASN1Sequence seq, String targetOid) throws IOException {
        for (int i = 0; i < seq.size(); i++) {
            ASN1Encodable element = seq.getObjectAt(i);
            if (element instanceof ASN1Sequence innerSeq && innerSeq.size() >= 2) {
                String oid = innerSeq.getObjectAt(0).toString();
                if (targetOid.equals(oid)) {
                    ASN1Encodable valueSet = innerSeq.getObjectAt(1);
                    if (valueSet instanceof ASN1Set set && set.size() > 0) {
                        return set.getObjectAt(0).toString();
                    }
                    if (valueSet instanceof ASN1Sequence valSeq && valSeq.size() > 0) {
                        return valSeq.getObjectAt(0).toString();
                    }
                    return valueSet.toString();
                }
            }
        }
        return null;
    }
}
