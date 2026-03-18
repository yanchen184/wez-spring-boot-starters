# common-security-test

整合測試模組 — 213 個測試，TDD 規格文件風格，13 個 Phase 完整覆蓋。

---

## 功能總覽

- **213 個整合測試** — 從資料層到 API 層全覆蓋
- **TDD Phase 分層** — 每個 Phase 測試一個關注面，由底層往上
- **規格文件風格** — 測試名稱即規格，讀測試就知道系統行為
- **Docker 測試環境** — MSSQL + Redis + OpenLDAP

---

## 測試 Phase 總覽

| Phase | 測試類別 | 測試數 | 關注面 |
|-------|---------|--------|--------|
| 1 | `Phase1_DataLayerTest` | 13 | Entity / Repository CRUD |
| 2 | `Phase2_UserDetailsServiceTest` | 11 | CustomUserDetailsService 載入使用者 |
| 3 | `Phase3_PasswordEncoderTest` | 10 | BCrypt + Legacy SHA-512 雙編碼 |
| 4 | `Phase4_LoginFlowTest` | 10 | 登入流程（成功/失敗/鎖定） |
| 5 | `Phase5_JwtTokenTest` | 14 | JWT 產生/驗證/Refresh/Claims |
| 6 | `Phase6_RbacPermissionTest` | 13 | RBAC 權限模型 + CRUD 權限評估 |
| 7 | `Phase7_RedisBlacklistTest` | 6 | Redis Token 黑名單 |
| 8 | `Phase8_AuthControllerIntegrationTest` | 10 | AuthController HTTP API |
| 9 | `Phase9_AutoConfigurationTest` | 7 | AutoConfiguration Bean 載入 |
| 10 | `Phase10_OrgPermissionTest` | 25 | 多機構多角色權限 |
| 11 | `Phase11_OtpTotpTest` | 41 | TOTP 設定/驗證/登入 |
| 12 | `Phase12_CaptchaTest` | 22 | 圖形/語音驗證碼 |
| 13 | `Phase13_CitizenCertTest` | 31 | 自然人憑證認證 |
| | **合計** | **213** | |

---

## 如何跑測試

### 前置條件

需要 Docker 啟動測試環境（MSSQL + Redis + OpenLDAP）：

```bash
cd care-security/config
docker-compose up -d
```

### 跑全部測試

```bash
cd care-security
mvn test -pl common-security-test
```

### 跑單一 Phase

```bash
# 只跑 Phase 4 登入流程
mvn test -pl common-security-test -Dtest=Phase4_LoginFlowTest

# 只跑 Phase 11 OTP
mvn test -pl common-security-test -Dtest=Phase11_OtpTotpTest
```

---

## 測試環境需求

| 服務 | 用途 | Docker |
|------|------|--------|
| MSSQL | 資料庫（Entity、Repository） | `mcr.microsoft.com/mssql/server` |
| Redis | Token 黑名單、驗證碼、挑戰碼 | `redis:7` |
| OpenLDAP | LDAP 認證測試（Phase 10+） | `bitnami/openldap` |

### 測試 Profile

測試使用 `@ActiveProfiles("local")` 載入 `application-local.yml`：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("local")
public @interface CareSecurityTest {
}
```

---

## Phase 細節

### Phase 1: Data Layer (13 tests)

- SaUser CRUD
- Role / Perm / Menu / Organize CRUD
- SaUserOrgRole 三方關聯
- AuditLog / LoginHistory / PwdHistory
- RolePerms 複合主鍵

### Phase 2: UserDetailsService (11 tests)

- 載入使用者 + 權限
- 不存在的使用者
- 帳號鎖定/停用
- CRUD 權限組裝

### Phase 3: PasswordEncoder (10 tests)

- BCrypt 編碼/驗證
- Legacy Grails SHA-512 驗證
- SmartMatchingEncoder 自動偵測
- DelegatingPasswordEncoder 前綴

### Phase 4: Login Flow (10 tests)

- 帳密正確登入
- 帳密錯誤
- 帳號鎖定
- 連續失敗鎖定
- LDAP 認證（模擬）

### Phase 5: JWT Token (14 tests)

- Access Token 產生
- Refresh Token 產生
- Token 解碼驗證
- Claims 內容（roles, userId, cname）
- Token 過期

### Phase 6: RBAC Permission (13 tests)

- 角色權限指派
- CrudPermissionEvaluator 評估
- hasPermission 方法
- 權限矩陣

### Phase 7: Redis Blacklist (6 tests)

- Token 加入黑名單
- 黑名單查詢
- TTL 過期

### Phase 8: AuthController Integration (10 tests)

- POST /api/auth/login HTTP 測試
- POST /api/auth/refresh
- POST /api/auth/logout
- GET /api/auth/me
- 錯誤處理

### Phase 9: AutoConfiguration (7 tests)

- Bean 是否正確載入
- 條件式配置
- @ConditionalOnMissingBean 覆蓋

### Phase 10: Org Permission (25 tests)

- 多機構角色
- 機構切換
- 機構級權限
- 代理登入
- 退出代理

### Phase 11: OTP / TOTP (41 tests)

- TotpService 單元測試（generateSecret, verifyCode, buildUri）
- OtpService 流程測試（setup, verify, enable, disable）
- OtpController API 測試
- 登入 OTP 流程（requiresOtp → verify → JWT）
- 時間容錯測試

### Phase 12: Captcha (22 tests)

- 驗證碼產生
- 驗證碼圖片 Base64
- 驗證碼驗證（正確/錯誤/過期/一次性）
- 語音驗證碼
- CaptchaController API 測試
- 登入整合（帶 captchaId + captchaAnswer）

### Phase 13: Citizen Cert (31 tests)

- LoginTokenService（產生/消費/過期）
- PKCS#7 解析
- 憑證驗證（有效期/憑證鏈/撤銷）
- MoicaCertUtils（CN/IDNO/SN 提取）
- CitizenCertController API 測試
- 使用者自動同步
- 完整登入流程

---

## 技術規格

| 項目 | 值 |
|------|-----|
| 測試框架 | JUnit 5 + Spring Boot Test |
| 測試類型 | `@SpringBootTest`（完整 Application Context） |
| 測試 Profile | `local`（Docker 環境） |
| 資料庫 | MSSQL（Hibernate auto DDL） |
