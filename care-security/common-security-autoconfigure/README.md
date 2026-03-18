# common-security-autoconfigure

自動配置模組 — 零配置即可用，一個 `@AutoConfiguration` 搞定所有 Bean 註冊。

---

## 功能總覽

- **自動註冊所有核心 Bean** — Service、Security、Controller、JPA、Redis
- **條件式啟用** — `care.security.enabled=true`（預設開啟）
- **所有 Bean 可覆蓋** — `@ConditionalOnMissingBean`，消費端可自訂
- **SPI 可選注入** — LDAP、OTP、CAPTCHA 未啟用時不會報錯

---

## 啟用條件

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "care.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CareSecurityProperties.class)
@ComponentScan(basePackages = {"com.company.common.security.controller", "com.company.common.security.exception"})
@EntityScan(basePackages = "com.company.common.security.entity")
@EnableJpaRepositories(basePackages = "com.company.common.security.repository")
public class CareSecurityAutoConfiguration { ... }
```

- `matchIfMissing = true`：未設定任何配置時**預設啟用**
- 設定 `care.security.enabled=false` 可完全關閉

---

## 自動註冊的 Bean

### Config Bean

| Bean | 條件 | 說明 |
|------|------|------|
| `OpenApiConfig` | classpath 有 springdoc | Swagger UI 分組設定 |
| `RedisConfig` | - | RedisTemplate 序列化配置 |
| `PasswordEncoderConfig` | - | BCrypt + Legacy 雙編碼器 |
| `AuthorizationServerConfig` | - | OAuth2 AS 設定（JWK、Client） |
| `CorsConfig` | - | CORS 來源設定 |
| `SecurityConfig` | - | 3 層 Filter Chain |

### Security Bean

| Bean | 說明 |
|------|------|
| `CustomUserDetailsService` | 載入使用者 + 權限 |
| `CrudPermissionEvaluator` | CRUD 權限評估器 |
| `JwtTokenCustomizer` | JWT 附加 claims |
| `LoginAttemptService` | 登入失敗鎖定 |
| `RedisTokenBlacklistService` | Token 黑名單 |

### Service Bean

| Bean | 說明 |
|------|------|
| `AuthService` | 認證核心（可選注入 LDAP/OTP/CAPTCHA） |
| `UserService` | 使用者管理 |
| `RoleService` | 角色管理 |
| `PermService` | 權限管理 |
| `MenuService` | 選單管理 |
| `OrganizeService` | 機構管理 |
| `OrgRoleService` | 機構角色管理 |
| `AuditService` | 審計日誌 |
| `PasswordHistoryService` | 密碼歷史 |
| `PasswordPolicyService` | 密碼策略 |

### OAuth2 Bean

| Bean | 說明 |
|------|------|
| `RegisteredClientRepository` | OAuth2 Client 註冊（InMemory） |
| `JWKSource` | RSA 金鑰（可持久化到檔案） |
| `JwtDecoder` | JWT 解碼器 |
| `JwtEncoder` | JWT 編碼器（NimbusJwtEncoder） |
| `AuthorizationServerSettings` | AS 端點設定 |
| `OAuth2TokenCustomizer` | Token 自訂器 |

### Security Filter Chain

| Bean | Order | 說明 |
|------|-------|------|
| `authorizationServerFilterChain` | 1 | OAuth2 端點 |
| `resourceServerFilterChain` | 2 | `/api/**` JWT 驗證 |
| `defaultFilterChain` | 3 | 其他請求 |

---

## CareSecurityProperties 配置項

前綴：`care.security`

### 完整 YAML 範例

```yaml
care:
  security:
    enabled: true                           # 總開關（預設 true）

    jwt:
      access-token-ttl-minutes: 30          # Access Token 有效期（分鐘，預設 30）
      refresh-token-ttl-days: 7             # Refresh Token 有效期（天，預設 7）
      keystore-path: ./keys/jwt-keys.json   # RSA 金鑰檔路徑（預設 ./keys/jwt-keys.json）
                                            # 檔案不存在會自動產生
                                            # 留空 = 記憶體金鑰（重啟失效）

    login:
      max-attempts: 5                       # 最大連續失敗次數（預設 5）
      lock-duration-minutes: 30             # 帳號鎖定時間（分鐘，預設 30）

    cors:
      allowed-origins: http://localhost:3000 # CORS 允許來源（預設 localhost:3000）

    password:
      min-length: 8                         # 最小長度（預設 8）
      max-length: 128                       # 最大長度（預設 128，防 DoS）
      require-uppercase: true               # 需要大寫字母（預設 true）
      require-lowercase: true               # 需要小寫字母（預設 true）
      require-digit: true                   # 需要數字（預設 true）
      require-special-char: true            # 需要特殊字元（預設 true）
      reject-sequential: true               # 禁止連續字元如 abc、123（預設 true）
      reject-repeated: true                 # 禁止重複字元如 aaa（預設 true）
      reject-common-weak: true              # 禁止常見弱密碼（預設 true）
      history-count: 5                      # 密碼歷史檢查筆數（預設 5，0=關閉）

    ldap:
      enabled: false                        # 啟用 LDAP 認證（預設 false）
      url: ldap://localhost:389             # LDAP 伺服器 URL
      base-dn: dc=example,dc=com           # Base DN
      user-search-filter: (sAMAccountName={0})  # 使用者搜尋 Filter
      bind-dn: cn=admin,dc=example,dc=com  # 服務帳號 DN
      bind-password: ""                     # 服務帳號密碼
      display-name-attr: displayName        # 顯示名稱屬性
      email-attr: mail                      # Email 屬性
      default-roles:                        # LDAP 使用者預設角色
        - ROLE_USER

    otp:
      enabled: false                        # 啟用 TOTP 兩步驟驗證（預設 false）
      issuer: CareSecuritySystem            # OTP 發行者名稱（顯示在 App 上）
      allowed-skew: 1                       # 允許的時間偏移步數（1 = +/- 30 秒）

    captcha:
      enabled: false                        # 啟用圖形驗證碼（預設 false）
      length: 4                             # 驗證碼長度（預設 4）
      chars: null                           # 自訂字元集（null = 依 includeLetters 決定）
      include-letters: false                # 包含英文字母（預設 false = 純數字）
      width: 160                            # 圖片寬度 px（預設 160）
      height: 50                            # 圖片高度 px（預設 50）
      font-size: 32                         # 字型大小 px（預設 32）
      audio-enabled: false                  # 啟用音訊驗證碼（無障礙，預設 false）
      expire-seconds: 300                   # 驗證碼過期秒數（預設 300）

    citizen-cert:
      enabled: false                        # 啟用自然人憑證認證（預設 false）
      challenge-expire-seconds: 300         # 挑戰碼過期秒數（預設 300）
      auto-create-user: true                # 自動建立使用者帳號（預設 true）
      default-roles:                        # 憑證使用者預設角色
        - ROLE_USER
      intermediate-cert-paths:              # 中繼憑證路徑
        - classpath:moica/MOICA2.cer
        - classpath:moica/MOICA3.cer
      local-crl-paths:                      # 本地 CRL 路徑
        - classpath:moica/MOICA2.crl
        - classpath:moica/MOICA3.crl
      ocsp-enabled: true                    # 啟用 OCSP 檢查（預設 true）
      crl-enabled: true                     # 啟用 CRL 檢查（預設 true）
      crl-cache-ttl-hours: 1                # CRL 快取時間（小時，預設 1）
```

---

## 覆蓋預設 Bean

所有 Bean 都有 `@ConditionalOnMissingBean`，消費端可自訂：

```java
@Configuration
public class MySecurityConfig {

    // 覆蓋預設的 PasswordEncoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(14);
    }

    // 覆蓋預設的 LoginAttemptService
    @Bean
    public LoginAttemptService loginAttemptService(SaUserRepository repo) {
        return new LoginAttemptService(repo, 10, 60); // 10 次失敗，鎖 60 分鐘
    }
}
```

---

## 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 自動配置 | `AutoConfiguration.imports`（Spring Boot 3.0+） |
| 配置前綴 | `care.security` |
