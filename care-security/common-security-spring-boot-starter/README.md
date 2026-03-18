# common-security-spring-boot-starter

Starter 空殼模組 — 一個依賴搞定所有安全功能。

---

## 用途

這是一個**純依賴聚合模組**，不包含任何程式碼。引入後自動帶入所有子模組。

---

## 引入方式

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-security-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

一行搞定，不需要分別引入各個模組。

---

## 包含的模組

| 模組 | 說明 |
|------|------|
| `common-security-autoconfigure` | 自動配置（含 core） |
| `common-security-auth-ldap` | LDAP 認證 |
| `common-security-auth-moica` | 自然人憑證認證 |
| `common-security-auth-otp` | TOTP 兩步驟驗證 |
| `common-security-auth-captcha` | 圖形驗證碼 |

### 依賴關係

```
common-security-spring-boot-starter
├── common-security-autoconfigure
│   └── common-security-core          ← 核心模組（Entity、Service、Security）
├── common-security-auth-ldap
├── common-security-auth-moica
├── common-security-auth-otp
└── common-security-auth-captcha
```

---

## 最小配置

引入後**零配置即可使用**基礎功能（登入/登出/JWT/RBAC）。

擴充功能需在 `application.yml` 中明確啟用：

```yaml
care:
  security:
    # 基礎功能預設啟用，不需設定

    # 以下功能需明確啟用
    ldap:
      enabled: true               # LDAP 認證
    otp:
      enabled: true               # TOTP 兩步驟驗證
    captcha:
      enabled: true               # 圖形驗證碼
    citizen-cert:
      enabled: true               # 自然人憑證
```

---

## 按需引入

如果只需要部分功能，可以跳過 starter，直接引入需要的模組：

```xml
<!-- 只要基礎功能（登入/登出/JWT/RBAC） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-security-autoconfigure</artifactId>
</dependency>

<!-- 加上 LDAP（可選） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-security-auth-ldap</artifactId>
</dependency>

<!-- 加上 CAPTCHA（可選） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-security-auth-captcha</artifactId>
</dependency>
```

---

## 完整配置參考

所有配置項請參考 [common-security-autoconfigure/README.md](../common-security-autoconfigure/README.md)。
