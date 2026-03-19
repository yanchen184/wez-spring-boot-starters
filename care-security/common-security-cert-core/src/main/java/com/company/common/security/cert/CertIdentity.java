package com.company.common.security.cert;

/**
 * Identity extracted from a certificate by a {@link CertProvider}.
 *
 * @param certType    the certificate type
 * @param id          the extracted identifier (IDNO last4, OID, BID, etc.)
 * @param commonName  the subject CN (person or organization name)
 * @param serialNumber the certificate serial number (hex)
 */
public record CertIdentity(
        CertType certType,
        String id,
        String commonName,
        String serialNumber
) {}
