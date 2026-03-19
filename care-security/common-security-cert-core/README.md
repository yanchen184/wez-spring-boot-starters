# common-security-cert-core

台灣政府憑證驗證共用基礎模組，提供 PKCS#7 驗簽、X.509 憑證鏈驗證、撤銷檢查（OCSP/CRL）、憑證身份提取等共用邏輯。

## 支援的憑證類型

| 類型 | 簡稱 | Issuer 關鍵字 | 身份提取 OID | 回傳 |
|------|------|-------------|-------------|------|
| 自然人憑證 | MOICA | 內政部憑證管理中心 | `2.16.886.1.100.2.51` | 身分證後 4 碼 |
| 政府憑證 | GCA | 政府憑證管理中心 | `2.16.886.1.100.2.102` | 機關 OID |
| 組織/團體憑證 | XCA | 組織及團體憑證管理中心 | `2.16.886.1.100.2.102` | 組織 OID |
| 工商憑證 | MOEACA | 工商憑證管理中心 | `2.16.886.1.100.2.101` | 統一編號 |

## 架構

```
cert-core（本模組）
├── CertType          — 憑證類型 enum（自動識別 issuer）
├── CertProvider      — SPI 介面（各 auth 模組實作）
├── CertFactory       — 根據 issuer 路由到正確 Provider
├── CertIdentity      — 提取結果（type, id, commonName, serialNumber）
├── Pkcs7Verifier     — PKCS#7 SignedData 驗簽 + 憑證提取
├── CertVerifier      — 憑證鏈驗證 + 有效期 + OCSP/CRL 撤銷檢查
└── CertExtensionUtils — X.509 extension 提取工具

auth-moica（依賴 cert-core）
└── MoicaCertProvider implements CertProvider

auth-gca（依賴 cert-core，未來）
└── GcaCertProvider implements CertProvider

auth-moeaca（依賴 cert-core，未來）
└── MoeacaCertProvider implements CertProvider

auth-xca（依賴 cert-core，未來）
└── XcaCertProvider implements CertProvider
```

## 使用方式

### 1. PKCS#7 驗簽

```java
Pkcs7Verifier verifier = new Pkcs7Verifier(base64SignedData, loginToken);

// 驗證簽章
boolean valid = verifier.verify();

// 提取憑證
X509Certificate cert = verifier.extractCertificate();
```

### 2. 憑證驗證（chain + validity + 撤銷）

```java
CertVerifier certVerifier = new CertVerifier(
    certificate,
    intermediateCerts,   // 中繼 CA 憑證列表
    localCrlPaths,       // 本地 CRL 路徑（可選）
    true,                // OCSP 啟用
    true                 // CRL 啟用
);

// 完整驗證（失敗拋 CertVerificationException）
certVerifier.fullVerify();

// 或分步驗證
certVerifier.checkValidity();
certVerifier.validateChain();
certVerifier.isRevoked();
```

### 3. 身份識別（透過 CertFactory）

```java
// CertFactory 自動收集所有 CertProvider
CertFactory certFactory = new CertFactory(List.of(
    moicaCertProvider,
    gcaCertProvider
));

// 根據 issuer 自動路由到正確 Provider
CertIdentity identity = certFactory.identify(certificate);
identity.certType();    // MOICA
identity.id();          // "6789"（身分證後 4 碼）
identity.commonName();  // "王小明"
identity.serialNumber(); // "A1B2C3D4"
```

### 4. 實作新的 CertProvider

```java
public class GcaCertProvider implements CertProvider {

    @Override
    public CertType getCertType() {
        return CertType.GCA;
    }

    @Override
    public String extractId(X509Certificate certificate) {
        // OID 2.16.886.1.100.2.102 = 機關 OID
        return CertExtensionUtils.extractSubjectDirectoryAttribute(
                certificate, "2.16.886.1.100.2.102");
    }
}
```

## CRL 快取

- 本地 CRL 和網路下載的 CRL 都使用 TTL 快取（預設 1 小時）
- 外部排程更新 CRL 檔案後，TTL 過期自動重新讀取
- 可調整：`CertVerifier.setCrlCacheTtlHours(2)`

## 依賴

- Bouncy Castle `bcpkix-jdk18on` 1.79
- SLF4J（日誌）
- 無 Spring 依賴（純 Java，可用於任何環境）
