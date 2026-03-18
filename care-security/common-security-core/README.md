# common-security-core

核心模組 — 所有業務邏輯、Entity、Service、Security 元件、REST API 都在這裡。

其他模組（captcha、ldap、otp、moica）透過 SPI 介面與 core 解耦。

---

## 功能總覽

- **RBAC 權限模型** — 使用者、角色、權限、選單、機構，支援多機構多角色
- **JWT 認證** — OAuth2 Authorization Server + Resource Server，RSA 簽章
- **登入防護** — 帳號鎖定、登入歷史、密碼歷史、Redis Token 黑名單
- **密碼策略** — 複雜度驗證、歷史檢查、BCrypt + Legacy Grails SHA-512 雙編碼
- **CRUD 權限評估** — `@PreAuthorize("hasPermission(#id, 'user', 'read')")` 方法級權限
- **審計日誌** — 所有操作自動記錄（登入、登出、密碼變更、使用者管理）
- **SPI 擴充** — CaptchaVerifier、LdapAuthenticator、LdapUserSyncer、OtpChecker

---

## Package 結構

```
com.company.common.security/
├── entity/                  # JPA Entity（10 個）
├── repository/              # Spring Data JPA Repository
├── service/                 # 業務服務
├── security/                # Security 元件（UserDetails、JWT、權限評估）
├── controller/              # REST API Controller
├── config/                  # Security Filter Chain、CORS、PasswordEncoder
├── spi/                     # SPI 介面（供擴充模組實作）
├── password/                # 密碼策略系統
├── validation/              # 自訂驗證註解
├── dto/
│   ├── request/             # Request DTO
│   └── response/            # Response DTO
└── exception/               # 統一例外處理
```

---

## Entity

| Entity | Table | 說明 |
|--------|-------|------|
| `SaUser` | SA_USER | 使用者（帳號、密碼、OTP、鎖定狀態） |
| `Role` | ROLE | 角色（ROLE_ADMIN, ROLE_USER 等） |
| `Perm` | PERM | 權限（resource:action 格式，如 `user:read`） |
| `Menu` | MENU | 選單（樹狀結構，parent_id 自關聯） |
| `Organize` | ORGANIZE | 機構（樹狀結構，支援多層級） |
| `SaUserOrgRole` | SA_USER_ORG_ROLE | 使用者-機構-角色 三方關聯 |
| `RolePerms` | ROLE_PERMS | 角色-權限 多對多（複合主鍵） |
| `AuditLog` | AUDIT_LOG | 審計日誌（操作類型、IP、User-Agent） |
| `LoginHistory` | LOGIN_HISTORY | 登入歷史 |
| `PwdHistory` | PWD_HISTORY | 密碼歷史（防止重複使用） |

### 關聯模型

```
SaUser ──< SaUserOrgRole >── Organize
                │
                └── Role ──< RolePerms >── Perm
                                            │
                                            └── Menu
```

---

## Service

| Service | 說明 |
|---------|------|
| `AuthService` | 登入/登出/Refresh/換密碼/切換使用者/切換機構 |
| `UserService` | 使用者 CRUD、分頁搜尋、鎖定/解鎖、重設密碼、機構角色指派 |
| `RoleService` | 角色查詢、權限矩陣管理 |
| `PermService` | 權限 CRUD |
| `MenuService` | 選單 CRUD、樹狀結構查詢 |
| `OrganizeService` | 機構查詢、樹狀結構 |
| `OrgRoleService` | 機構-角色指派查詢（按機構分組） |
| `AuditService` | 審計日誌、登入歷史記錄 |
| `PasswordHistoryService` | 密碼歷史檢查（防止重複使用最近 N 組密碼） |

---

## Security 元件

| 元件 | 說明 |
|------|------|
| `CustomUserDetails` | 擴充 `UserDetails`，帶 userId、cname、orgId、CRUD 權限 |
| `CustomUserDetailsService` | 載入使用者 + 權限，組裝 `CustomUserDetails` |
| `JwtTokenCustomizer` | 在 JWT 中嵌入 roles、userId、cname、currentOrgId |
| `LoginAttemptService` | 連續失敗 N 次鎖定帳號（Redis 計數） |
| `RedisTokenBlacklistService` | Token 登出後加入黑名單，防止重複使用 |
| `CrudPermissionEvaluator` | 實作 `PermissionEvaluator`，支援 `hasPermission(#id, 'resource', 'action')` |

---

## SPI 介面

core 定義介面，擴充模組提供實作。AuthService 透過 `ObjectProvider` 可選注入。

| 介面 | 實作模組 | 說明 |
|------|---------|------|
| `CaptchaVerifier` | common-security-auth-captcha | `verifyCaptcha(captchaId, answer)` |
| `LdapAuthenticator` | common-security-auth-ldap | `authenticate(username, password)` → `LdapUserResult` |
| `LdapUserSyncer` | common-security-auth-ldap | `syncUser(ldapUser)` → 本地 `SaUser` |
| `OtpChecker` | common-security-auth-otp | `isOtpEnabled(username)` |

```java
// 未啟用 LDAP 時，ldapAuthenticator 為 null，login 走本地驗證
@Bean
public AuthService authService(
        ObjectProvider<LdapAuthenticator> ldapAuthProvider,
        ObjectProvider<OtpChecker> otpServiceProvider,
        ObjectProvider<CaptchaVerifier> captchaServiceProvider,
        ...) {
    return new AuthService(...,
            ldapAuthProvider.getIfAvailable(),
            otpServiceProvider.getIfAvailable(),
            ...);
}
```

---

## REST API

### AuthController (`/api/auth`)

| 方法 | 路徑 | 說明 | 權限 |
|------|------|------|------|
| POST | `/api/auth/login` | 登入，回傳 JWT | 公開 |
| POST | `/api/auth/refresh` | 刷新 access token | 公開 |
| POST | `/api/auth/logout` | 登出（Token 加入黑名單） | 已認證 |
| POST | `/api/auth/change-password` | 變更密碼 | 已認證 |
| GET | `/api/auth/me` | 取得當前使用者資訊 | 已認證 |
| GET | `/api/auth/my-orgs` | 取得使用者所屬機構清單 | 已認證 |
| POST | `/api/auth/switch-user` | 代理登入（模擬其他使用者） | ROLE_ADMIN |
| POST | `/api/auth/exit-switch-user` | 退出代理登入 | 已認證 |

### UserController (`/api/users`)

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/users` | 分頁搜尋使用者（keyword、orgId） |
| GET | `/api/users/{id}` | 取得單一使用者 |
| POST | `/api/users` | 建立使用者 |
| PUT | `/api/users/{id}` | 更新使用者 |
| POST | `/api/users/{id}/lock` | 鎖定帳號 |
| POST | `/api/users/{id}/unlock` | 解鎖帳號 |
| POST | `/api/users/{id}/reset-password` | 重設密碼 |
| GET | `/api/users/{id}/org-roles` | 取得使用者的機構角色 |
| POST | `/api/users/{id}/org-roles` | 指派機構角色 |
| DELETE | `/api/users/{id}/org-roles/{orgRoleId}` | 移除機構角色 |

### RoleController (`/api/roles`)

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/roles` | 列出所有角色 |
| GET | `/api/roles/{id}` | 取得單一角色 |
| GET | `/api/roles/{id}/permissions` | 取得權限矩陣 |
| PUT | `/api/roles/{id}/permissions` | 更新角色權限 |

### PermController (`/api/perms`)

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/perms` | 列出所有權限 |
| POST | `/api/perms` | 建立權限 |
| PUT | `/api/perms/{id}` | 更新權限 |
| DELETE | `/api/perms/{id}` | 刪除權限 |

### MenuController (`/api/menus`)

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/menus` | 列出所有選單（平面） |
| GET | `/api/menus/tree` | 取得選單樹 |
| POST | `/api/menus` | 建立選單 |
| PUT | `/api/menus/{id}` | 更新選單 |
| DELETE | `/api/menus/{id}` | 刪除選單 |

### OrganizeController (`/api/orgs`)

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/orgs` | 列出所有機構（平面） |
| GET | `/api/orgs/tree` | 取得機構樹 |

### OrgRoleController (`/api/org-roles`)

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/org-roles` | 取得所有機構-角色指派（按機構分組） |

### PasswordPolicyController (`/api/v1/password-policy`)

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/v1/password-policy` | 取得目前密碼策略 |
| GET | `/api/v1/password-policy/providers` | 列出策略提供者 |

---

## 密碼編碼器

`PasswordEncoderConfig` 使用 `DelegatingPasswordEncoder` 支援雙編碼：

| 編碼器 | 用途 | 格式 |
|--------|------|------|
| `Password4jBcryptEncoder` | **新密碼**（預設） | `{bcrypt}$2b$12$...` |
| `LegacyGrailsPasswordEncoder` | **舊密碼**（Grails 遷移，唯讀） | `{SHA-512}{base64salt}hexhash` |
| `SmartMatchingEncoder` | **自動偵測**（無前綴時） | 依格式自動選擇 BCrypt 或 SHA-512 |

```
新使用者  → BCrypt 編碼
舊使用者  → 登入時 SHA-512 驗證成功 → 自動升級為 BCrypt（規劃中）
```

---

## Security Filter Chain 架構

3 層 Filter Chain，依 `@Order` 優先級處理：

```
@Order(1) — Authorization Server Filter Chain
  → 處理 OAuth2 端點（/oauth2/**、/.well-known/**）

@Order(2) — Resource Server Filter Chain
  → 處理 /api/** 請求，JWT 驗證
  → /api/auth/login, /api/auth/refresh 等公開端點放行
  → /api/users/** 需要 ADMIN 或 USER_ADMIN
  → /api/roles/**, /api/permissions/** 需要 ADMIN

@Order(3) — Default Filter Chain
  → 處理其他請求（Actuator、Swagger、靜態資源）
  → /actuator/health、/swagger-ui/** 放行
```

---

## 密碼策略系統

支援多來源策略提供者（SPI），優先級最高的生效：

```java
// 自訂策略提供者（優先級比 YAML 高）
@Component
public class DbPasswordPolicyProvider implements PasswordPolicyProvider {
    @Override
    public int getOrder() { return 1; } // 數字越小優先級越高

    @Override
    public PasswordPolicyConfig getPolicy() {
        // 從 DB 載入策略
    }
}
```

內建的 `YamlPasswordPolicyProvider` 從 `care.security.password.*` 讀取。

---

## 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| Spring Security | OAuth2 Authorization Server + Resource Server |
| 密碼庫 | password4j（BCrypt） |
| JWT | Nimbus JOSE + JWT |
| ORM | Spring Data JPA / Hibernate |
| Redis | Token 黑名單、登入鎖定計數 |
