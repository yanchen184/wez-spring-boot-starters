# common-security-auth-ldap

LDAP 認證模組 — 支援 Active Directory / OpenLDAP，bind-and-search 驗證 + 自動同步使用者。

---

## 功能總覽

- **Bind-and-Search 認證** — 服務帳號搜尋使用者 DN，再用使用者帳密 bind 驗證
- **自動同步使用者** — LDAP 驗證成功後，自動建立/更新本地 SaUser
- **LDAP Filter 防注入** — 跳脫特殊字元（`\`、`*`、`(`、`)`、`\0`）
- **連線狀態查詢** — API 端點查詢 LDAP 連線狀態
- **SPI 整合** — 實作 `LdapAuthenticator` + `LdapUserSyncer` 介面

---

## 啟用方式

```yaml
care:
  security:
    ldap:
      enabled: true                          # 必須明確啟用
      url: ldap://10.1.2.100:389             # LDAP 伺服器
      base-dn: dc=company,dc=com            # Base DN
      bind-dn: cn=admin,dc=company,dc=com   # 服務帳號
      bind-password: secretPassword          # 服務帳號密碼
```

啟用後自動註冊 `LdapAuthenticationProvider`、`LdapUserSyncService`、`LdapController`。

---

## 登入流程

```
1. 使用者輸入帳號密碼
2. AuthService 偵測到 LdapAuthenticator 已注入
3. LdapAuthenticationProvider.authenticate(username, password)
   a. 用服務帳號 bind 到 LDAP
   b. 用 userSearchFilter 搜尋使用者 DN
   c. 用使用者 DN + 密碼 bind 驗證
   d. 驗證成功 → 回傳 LdapUserResult(username, displayName, email)
4. LdapUserSyncService.syncUser(ldapUser)
   → 本地不存在 → 建立 SaUser + 指派 defaultRoles
   → 本地已存在 → 更新 displayName、email
5. AuthService 產生 JWT token 回傳
```

---

## 核心類別

### LdapAuthenticationProvider

實作 `LdapAuthenticator` SPI 介面。

| 方法 | 說明 |
|------|------|
| `authenticate(username, password)` | Bind-and-Search 驗證，回傳 `Optional<LdapUserResult>` |
| `testConnection()` | 測試 LDAP 服務帳號連線 |

**安全特性：**
- LDAP Filter 字元跳脫（防注入）
- LDAPS (SSL) 自動偵測
- 連線逾時：connect 5 秒、read 10 秒

### LdapUserSyncService

實作 `LdapUserSyncer` SPI 介面。

| 方法 | 說明 |
|------|------|
| `syncUser(ldapUser)` | 同步 LDAP 使用者到本地 DB（新建或更新） |

---

## REST API

| 方法 | 路徑 | 說明 | 權限 |
|------|------|------|------|
| GET | `/api/ldap/status` | 查詢 LDAP 啟用狀態和連線狀態 | ROLE_ADMIN |

```
GET /api/ldap/status

Response:
{
  "enabled": true,
  "connected": true
}
```

---

## 配置項

前綴：`care.security.ldap`

```yaml
care:
  security:
    ldap:
      enabled: false                                 # 是否啟用（預設 false）
      url: ldap://localhost:389                      # LDAP URL（支援 ldaps://）
      base-dn: dc=example,dc=com                    # 搜尋基底 DN
      user-search-filter: (sAMAccountName={0})       # 使用者搜尋 Filter
      bind-dn: cn=admin,dc=example,dc=com           # 服務帳號 DN
      bind-password: ""                              # 服務帳號密碼
      display-name-attr: displayName                 # 顯示名稱屬性名
      email-attr: mail                               # Email 屬性名
      default-roles:                                 # LDAP 使用者預設角色
        - ROLE_USER
```

### 常見 LDAP Filter 範例

| 環境 | Filter | 說明 |
|------|--------|------|
| Active Directory | `(sAMAccountName={0})` | AD 帳號名稱 |
| Active Directory | `(userPrincipalName={0}@company.com)` | AD UPN 格式 |
| OpenLDAP | `(uid={0})` | 標準 LDAP uid |
| OpenLDAP | `(cn={0})` | Common Name |
| 限制搜尋範圍 | `(&(uid={0})(objectClass=inetOrgPerson))` | 僅搜尋特定 objectClass |

> `{0}` 會被替換為使用者輸入的帳號名稱，已做跳脫處理。

---

## SPI 介面

### LdapAuthenticator

```java
public interface LdapAuthenticator {
    Optional<LdapUserResult> authenticate(String username, String password);
    boolean testConnection();

    record LdapUserResult(String username, String displayName, String email) {}
}
```

### LdapUserSyncer

```java
public interface LdapUserSyncer {
    SaUser syncUser(LdapAuthenticator.LdapUserResult ldapUser);
}
```

---

## 安全考量

| 項目 | 做法 |
|------|------|
| LDAP Injection | `escapeFilter()` 跳脫 `\*()` 等特殊字元 |
| 密碼傳輸 | 建議使用 `ldaps://`（SSL/TLS）或 StartTLS |
| 服務帳號 | 僅需搜尋權限，不需要寫入權限 |
| 連線逾時 | connect 5s + read 10s，防止 LDAP 伺服器無回應 |
| Context 關閉 | `finally` 確保 `DirContext.close()`，防止連線洩漏 |

---

## 技術規格

| 項目 | 值 |
|------|-----|
| LDAP Library | JNDI（`javax.naming`，JDK 內建） |
| 認證方式 | Simple Bind（LDAP v3） |
| SSL 支援 | `ldaps://` 自動啟用 |
