package com.company.common.security.cert;

import java.security.cert.X509Certificate;

/**
 * SPI for certificate-type-specific identity extraction.
 * <p>
 * Each Taiwan government certificate type (MOICA, GCA, MOEACA, XCA) implements
 * this interface to extract its specific identifier from the certificate extensions.
 * <p>
 * Implementations are auto-discovered by {@link CertFactory}.
 */
public interface CertProvider {

    /**
     * @return the certificate type this provider handles
     */
    CertType getCertType();

    /**
     * Extract the identity-specific ID from the certificate.
     * <ul>
     *   <li>MOICA: last 4 digits of national ID (OID 2.16.886.1.100.2.51)</li>
     *   <li>GCA: government agency OID (OID 2.16.886.1.100.2.102)</li>
     *   <li>XCA: organization OID (OID 2.16.886.1.100.2.102)</li>
     *   <li>MOEACA: business ID / 統一編號 (OID 2.16.886.1.100.2.101)</li>
     * </ul>
     *
     * @param certificate the X.509 certificate
     * @return the extracted ID, or null if not found
     */
    String extractId(X509Certificate certificate);
}
