package com.company.common.security.cert.provider;

import com.company.common.security.cert.CertExtensionUtils;
import com.company.common.security.cert.CertProvider;
import com.company.common.security.cert.CertType;

import java.security.cert.X509Certificate;

/**
 * MOICA (自然人憑證) certificate provider.
 * <p>
 * Extracts the last 4 digits of the national ID number from
 * the certificate's Subject Directory Attributes extension.
 * <p>
 * OID: 2.16.886.1.100.2.51 — 身分證後 4 碼
 * <p>
 * Fallback: custom extension OID 2.16.886.1.100.1.1
 * (used by some MOICA certificate versions)
 */
public class MoicaCertProvider implements CertProvider {

    /** Subject Directory Attributes 內的身分證 OID */
    private static final String MOICA_IDNO_SDA_OID = "2.16.886.1.100.2.51";

    /** 自訂 extension OID（部分版本使用） */
    private static final String MOICA_IDNO_EXT_OID = "2.16.886.1.100.1.1";

    @Override
    public CertType getCertType() {
        return CertType.MOICA;
    }

    @Override
    public String extractId(X509Certificate certificate) {
        // 嘗試從 Subject Directory Attributes (OID 2.5.29.9) 提取
        String id = CertExtensionUtils.extractSubjectDirectoryAttribute(certificate, MOICA_IDNO_SDA_OID);
        if (id != null) {
            return last4(id);
        }

        // Fallback: 從自訂 extension OID 提取
        id = CertExtensionUtils.extractFromCustomExtension(certificate, MOICA_IDNO_EXT_OID);
        if (id != null) {
            return last4(id);
        }

        return null;
    }

    private static String last4(String value) {
        if (value != null && value.length() >= 4) {
            return value.substring(value.length() - 4);
        }
        return value;
    }
}
