# wez-crypto → common-crypto-starter

> 狀態：❌ 待建

## 原始模組分析

- **路徑**: `backend/wez-crypto`
- **檔案數**: 12 個（1 Java + 11 Groovy）
- **外部依賴**: BouncyCastle (`bcpkix-jdk18on`)、Guava Cache、jodd-datetime、`wez-core`

## 功能清單

| 類別 | 功能 |
|------|------|
| `KryptoUtils.java` | RSA 公私鑰讀取（PEM/KeyStore）、X509 憑證解析、hex 轉換 |
| `CertUtils.groovy` | 抽象基底：中繼憑證驗證、SHA256withRSA 簽章驗證、憑證過期/廢止檢查 |
| `CertFactory.groovy` | 依發行者名稱判斷憑證類型，回傳對應 CertUtils 實作 |
| `GcaCertUtils` | 政府憑證管理中心 |
| `XcaCertUtils` | 組織及團體憑證管理中心 |
| `MoeacaCertUtils` | 工商憑證管理中心 |
| `MoicaCertUtils` | 內政部憑證管理中心（自然人憑證） |
| `Pkcs7Utils` | PKCS#7 簽章處理 |
| `TrustAllCertificates` | 跳過 SSL 驗證（開發用） |

## Grails 耦合點（需改寫）

| 耦合 | 位置 | 改法 |
|------|------|------|
| `Holders.grailsApplication.mainContext.getBean('dataSource')` | `CertUtils:94` | 改用 `@Autowired DataSource` 或建構子注入 |
| `groovy.sql.Sql` 查 `CERT_REV_LIST` | `CertUtils:281` | 改用 `JdbcTemplate` 或 JPA Repository |
| `SessionUtils.session`（HttpSession） | `CertUtils:122-143` | 改用 Spring Security Context 或移到 Service 層 |
| Groovy 語法 | 全部 `.groovy` 檔 | 轉 Java 或保留 Groovy（加 groovy 依賴） |

## 遷移步驟

1. `KryptoUtils.java` → 直接搬，零修改
2. `CertUtils` 系列 → Groovy 轉 Java，DI 改 Spring 注入
3. Guava Cache → 可換 Caffeine 或 Spring Cache
4. jodd-datetime → 換 `java.time`
5. 撰寫 `CryptoAutoConfiguration` + `spring.factories`
