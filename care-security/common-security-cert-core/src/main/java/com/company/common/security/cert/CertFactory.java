package com.company.common.security.cert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory that routes a certificate to the correct {@link CertProvider}
 * based on the certificate's issuer DN.
 * <p>
 * Auto-discovers all registered CertProvider implementations (via constructor injection).
 * <p>
 * Usage:
 * <pre>
 * CertIdentity identity = certFactory.identify(x509Certificate);
 * // identity.certType()   → MOICA / GCA / MOEACA / XCA
 * // identity.id()         → last4IDNO / OID / BID
 * // identity.commonName() → 王小明
 * </pre>
 */
public class CertFactory {

    private static final Logger log = LoggerFactory.getLogger(CertFactory.class);

    private final Map<CertType, CertProvider> providerMap;

    public CertFactory(List<CertProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(CertProvider::getCertType, Function.identity()));
        log.info("CertFactory initialized with {} providers: {}",
                providerMap.size(), providerMap.keySet());
    }

    /**
     * Detect the certificate type and extract identity information.
     *
     * @param certificate the X.509 certificate
     * @return the extracted identity
     * @throws CertVerificationException if the certificate type is unrecognized or no provider is registered
     */
    public CertIdentity identify(X509Certificate certificate) {
        String issuerDn = certificate.getIssuerX500Principal().getName();
        CertType certType = CertType.fromIssuer(issuerDn);

        if (certType == null) {
            throw new CertVerificationException("無法識別的憑證類型，Issuer: " + issuerDn);
        }

        CertProvider provider = providerMap.get(certType);
        if (provider == null) {
            throw new CertVerificationException(
                    "未註冊的憑證 Provider: " + certType.getDisplayName() + "（請加入對應的 auth 模組依賴）");
        }

        String id = provider.extractId(certificate);
        String cn = CertExtensionUtils.getSubjectCName(certificate);
        String sn = CertExtensionUtils.getSerialNumberHex(certificate);

        log.debug("Certificate identified: type={}, cn={}, id={}, sn={}", certType, cn, id, sn);
        return new CertIdentity(certType, id, cn, sn);
    }

    /**
     * Check if a provider is registered for the given certificate type.
     */
    public boolean supports(CertType certType) {
        return providerMap.containsKey(certType);
    }
}
