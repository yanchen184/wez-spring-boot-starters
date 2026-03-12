# care-security-spring-boot-starter

Spring Boot 4.0 + Spring Security 7 的安全框架 Starter，提供 JWT 認證、RBAC 權限、帳號管理等功能，加一個依賴即可開箱即用。

## 技術棧

- Spring Boot 4.0.3 / Spring Security 7
- Spring Authorization Server (OAuth2 JWT)
- Spring Data JPA + Redis
- MSSQL (SQL Server)
- Password4j (SHA-512 密碼加密)
- SpringDoc OpenAPI 3 (Swagger UI)

## 模組結構

```
care-security/                          # Parent POM (Multi-module)
├── pom.xml
├── docs/                               # 技術文件
│
├── care-security-core/                 # 核心模組：所有業務邏輯
│   └── src/main/java/gov/mohw/care/security/
│       ├── config/                     # 框架配置
│       ├── controller/                 # REST API
│       ├── dto/                        # 請求/回應 DTO
│       ├── entity/                     # JPA Entity
│       ├── exception/                  # 全域例外處理
│       ├── repository/                 # Spring Data JPA Repository
│       ├── security/                   # 認證/授權核心元件
│       └── service/                    # 業務邏輯服務
│
├── care-security-autoconfigure/        # 自動配置模組
│   └── src/main/java/.../autoconfigure/
│       ├── CareSecurityAutoConfiguration.java
│       └── CareSecurityProperties.java
│
├── care-security-starter/              # Starter 空殼（只拉依賴）
│   └── pom.xml
│
└── care-security-test/                 # 測試模組 (95 個測試)
    └── src/test/java/.../test/
        ├── Phase1 ~ Phase9 測試
        └── TestApplication.java
```

### care-security-core

框架的核心程式碼，包含所有業務邏輯。

#### config/ — 框架配置

| 檔案 | 說明 |
|------|------|
| `SecurityConfig.java` | Spring Security FilterChain 配置（3 條鏈：Authorization Server、Resource Server、Default），整合 CORS 與 RBAC |
| `AuthorizationServerConfig.java` | OAuth2 Authorization Server 設定（JWT 簽發、Token 自訂欄位） |
| `CorsConfig.java` | CORS 跨域配置（允許來源從 `care.security.cors.allowed-origins` 讀取） |
| `PasswordEncoderConfig.java` | 密碼編碼器（SHA-512 + Base64 Salt，格式：`{SHA-512}{salt}hash`） |
| `RedisConfig.java` | Redis 序列化配置（String key + JSON value） |
| `OpenApiConfig.java` | Swagger UI 配置（Bearer Token 認證） |

#### controller/ — REST API

| 檔案 | 路徑前綴 | 說明 |
|------|----------|------|
| `AuthController.java` | `/api/auth` | 登入、登出、刷新 Token、修改密碼 |
| `UserController.java` | `/api/users` | 使用者 CRUD、鎖定/解鎖、重設密碼 |
| `RoleController.java` | `/api/roles` | 角色管理、權限矩陣 CRUD |
| `MenuController.java` | `/api/menus` | 選單樹查詢 |
| `OrganizeController.java` | `/api/orgs` | 組織樹查詢 |

#### dto/ — 資料傳輸物件

| 子目錄 | 檔案 | 說明 |
|--------|------|------|
| `request/` | `LoginRequest` | 登入（帳號 + 密碼） |
| | `RefreshTokenRequest` | 刷新 Token |
| | `LogoutRequest` | 登出（帶 access token） |
| | `ChangePasswordRequest` | 修改密碼（舊密碼 + 新密碼） |
| | `CreateUserRequest` | 建立使用者 |
| | `UpdateUserRequest` | 更新使用者 |
| | `ResetPasswordRequest` | 重設密碼（管理員操作） |
| | `RolePermissionRequest` | 更新角色的權限矩陣 |
| `response/` | `ApiResponse<T>` | 統一 API 回應格式 |
| | `TokenResponse` | JWT Token 回應（access + refresh） |
| | `UserResponse` | 使用者資訊 |
| | `RoleResponse` | 角色資訊 |
| | `MenuTreeResponse` | 選單樹節點 |
| | `OrganizeTreeResponse` | 組織樹節點 |
| | `PermissionMatrixResponse` | 權限矩陣（選單 × CRUD） |

#### entity/ — JPA 實體（對應資料庫表）

| 檔案 | 對應表 | 說明 |
|------|--------|------|
| `SaUser.java` | SAUSER | 後台使用者帳號 |
| `Role.java` | ROLE | 角色 |
| `SaUserRole.java` | SAUSER_ROLE | 使用者-角色關聯（多對多） |
| `SaUserOrgRole.java` | SAUSER_ORG_ROLE | 使用者-組織-角色三方關聯 |
| `Perm.java` | PERM | 權限（CRUD 欄位） |
| `RolePerms.java` | ROLE_PERMS | 角色-權限關聯 |
| `Menu.java` | MENU | 選單（樹狀結構） |
| `Organize.java` | ORGANIZE | 組織（樹狀結構） |
| `PwdHistory.java` | PWD_HISTORY | 密碼歷史（防止重複使用） |
| `LoginHistory.java` | LOGIN_HISTORY | 登入紀錄 |
| `AuditLog.java` | AUDIT_LOG | 稽核日誌 |
| `RequestMap.java` | REQUESTMAP | URL-角色對應 |
| `base/AuditableEntity.java` | — | 審計基底類（建立/修改時間+人員） |
| `id/SaUserRoleId.java` | — | 複合主鍵 |
| `id/RolePermsId.java` | — | 複合主鍵 |

#### security/ — 認證授權核心

| 檔案 | 說明 |
|------|------|
| `CustomUserDetailsService.java` | 從 DB 載入使用者、角色、權限，轉為 Spring Security UserDetails |
| `CustomUserDetails.java` | 自訂 UserDetails（含組織角色、權限碼） |
| `JwtTokenCustomizer.java` | 在 JWT 中加入 userId、roles、permissions 等自訂欄位 |
| `LoginAttemptService.java` | 登入失敗次數追蹤、帳號鎖定（使用 Redis） |
| `RedisTokenBlacklistService.java` | JWT 黑名單（登出後 Token 立即失效） |
| `CrudPermissionEvaluator.java` | RBAC 權限評估器（支援 `@PreAuthorize("hasPermission('SC900','READ')")` 語法） |

#### service/ — 業務邏輯

| 檔案 | 說明 |
|------|------|
| `AuthService.java` | 登入驗證、JWT 簽發/刷新、登出、改密碼 |
| `UserService.java` | 使用者 CRUD、鎖定/解鎖、重設密碼 |
| `RoleService.java` | 角色查詢、權限矩陣管理 |
| `MenuService.java` | 選單樹建構 |
| `OrganizeService.java` | 組織樹建構 |
| `AuditService.java` | 稽核日誌寫入 |

#### repository/ — 資料存取

| 檔案 | 說明 |
|------|------|
| `SaUserRepository.java` | 使用者查詢（含 by account） |
| `RoleRepository.java` | 角色查詢 |
| `SaUserRoleRepository.java` | 使用者-角色關聯 |
| `SaUserOrgRoleRepository.java` | 使用者-組織-角色關聯 |
| `PermRepository.java` | 權限查詢 |
| `RolePermsRepository.java` | 角色-權限關聯 |
| `MenuRepository.java` | 選單查詢 |
| `OrganizeRepository.java` | 組織查詢 |
| `PwdHistoryRepository.java` | 密碼歷史 |
| `LoginHistoryRepository.java` | 登入紀錄 |
| `AuditLogRepository.java` | 稽核日誌 |

#### exception/

| 檔案 | 說明 |
|------|------|
| `GlobalExceptionHandler.java` | 全域例外處理（統一回傳 `ApiResponse` 格式） |

### care-security-autoconfigure

Spring Boot 自動配置模組，讓使用者加依賴就能用。

| 檔案 | 說明 |
|------|------|
| `CareSecurityAutoConfiguration.java` | 自動註冊所有 Bean（SecurityConfig、CorsConfig、PasswordEncoder、Redis、JPA、Controller 等），支援 `@ConditionalOnMissingBean` 讓使用者覆寫 |
| `CareSecurityProperties.java` | 配置屬性類，綁定 `care.security.*` 前綴（JWT TTL、登入鎖定、CORS 等） |
| `META-INF/spring/...AutoConfiguration.imports` | Spring Boot 4 的自動配置註冊檔 |

### care-security-starter

空殼模組，只在 pom.xml 中引入 `care-security-autoconfigure`。使用者只需依賴這一個 artifact。

### care-security-test

95 個測試，分 9 個 Phase：

| Phase | 檔案 | 測試內容 |
|-------|------|----------|
| 1 | `Phase1_DataLayerTest.java` | Entity、Repository 資料層 |
| 2 | `Phase2_UserDetailsServiceTest.java` | UserDetailsService 載入使用者 |
| 3 | `Phase3_PasswordEncoderTest.java` | SHA-512 密碼編碼/驗證 |
| 4 | `Phase4_LoginFlowTest.java` | 登入流程（成功、失敗、鎖定） |
| 5 | `Phase5_JwtTokenTest.java` | JWT 簽發、解析、過期 |
| 6 | `Phase6_RbacPermissionTest.java` | RBAC 權限評估 |
| 7 | `Phase7_RedisBlacklistTest.java` | Redis Token 黑名單 |
| 8 | `Phase8_AuthControllerIntegrationTest.java` | AuthController 整合測試 |
| 9 | `Phase9_AutoConfigurationTest.java` | 自動配置 Bean 載入驗證 |

### docs/ — 技術文件

| 檔案 | 說明 |
|------|------|
| `01-Spring-Boot-4-技術選型報告.md` | Spring Boot 4 技術選型分析 |
| `02-PoC-審查報告.md` | 概念驗證審查 |
| `03-RBAC-重構方案.md` | RBAC 權限架構重構設計 |
| `TDD-Design-Specification.md` | TDD 測試規格書（9 Phase） |

## 使用方式

### 1. 安裝到 Local Maven Repository

```bash
export JAVA_HOME="C:/jdk21/jdk-21.0.10+7"
export PATH="/c/workspace/apache-maven-3.9.9/bin:$PATH"
mvn clean install -DskipTests
```

### 2. 在應用專案中引入

```xml
<dependency>
    <groupId>gov.mohw.care</groupId>
    <artifactId>care-security-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 3. 配置 application.yml

```yaml
care:
  security:
    enabled: true
    jwt:
      access-token-ttl-minutes: 30
      refresh-token-ttl-days: 7
    login:
      max-attempts: 5
      lock-duration-minutes: 30
    cors:
      allowed-origins: http://localhost:3000
```

完整的 Demo 專案請參考 [security-starter-demo](https://github.com/yanchen184/security-starter-demo)。

## 執行測試

```bash
mvn test    # 95 個測試全部通過
```
