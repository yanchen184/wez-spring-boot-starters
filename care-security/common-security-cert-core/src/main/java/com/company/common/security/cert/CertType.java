package com.company.common.security.cert;

/**
 * Taiwan government certificate types.
 */
public enum CertType {
    MOICA("MOICA", "自然人憑證", "內政部憑證管理中心"),
    GCA("GCA", "政府憑證", "政府憑證管理中心"),
    XCA("XCA", "組織及團體憑證", "組織及團體憑證管理中心"),
    MOEACA("MOEACA", "工商憑證", "工商憑證管理中心");

    private final String code;
    private final String displayName;
    private final String issuerKeyword;

    CertType(String code, String displayName, String issuerKeyword) {
        this.code = code;
        this.displayName = displayName;
        this.issuerKeyword = issuerKeyword;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getIssuerKeyword() { return issuerKeyword; }

    /**
     * Detect certificate type by examining the issuer DN.
     *
     * @return the matching CertType, or null if unrecognized
     */
    public static CertType fromIssuer(String issuerDn) {
        if (issuerDn == null) {
            return null;
        }
        for (CertType type : values()) {
            if (issuerDn.contains(type.issuerKeyword)) {
                return type;
            }
        }
        return null;
    }
}
