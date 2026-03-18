# Grails 5 → Spring Boot 遷移對照表

> 目標：Spring Boot 4.0.3 + Java 21 + Maven
> 更新日期：2026-03-17

## 遷移狀態總覽

| 狀態 | 數量 | 說明 |
|------|------|------|
| ✅ 已完成 | 11 個 | 已建立對應 starter |
| ✅ 原生取代 | 2 個 | 不做 starter，直接用 Spring Boot 原生功能 |
| 🟢 待建（容易） | 1 個 | 預估 1-2 天 |
| 🟡 待建（不容易） | 7 個 | 預估 3-7 天/個 |
| ⬜ 不用做 | 14 個 | 前後端分離淘汰 / 太專案化 |
| 🔵 獨立運作 | 2 個 | 不需遷移 |

## 已完成（9 個）

| Grails 模組 | Spring Boot 對應 | 備註 |
|---|---|---|
| `wez-logs` + `wez-logs-extra-filter` | `common-log-spring-boot-starter` | — |
| `wez-base-core`（Sauser, Role, Org, Menu, Perm） | `care-security` core | — |
| `wez-base`（基礎功能） | `common-jpa-spring-boot-starter` | 審計、軟刪除 |
| `wez-security`（認證/權限） | `care-security` | — |
| `wez-web-security`（密碼變更/重設） | `care-security` AuthService | — |
| `wez-captcha` + `wez-security-captcha` | `auth-captcha` | — |
| `wez-security-auth-otp` | `auth-otp` | — |
| `wez-security-auth-moica-*`（2 個） | `auth-moica` | 大致完成 |
| 統一回應/例外處理 | `common-response-spring-boot-starter` | — |
| `wez-attachment` | `common-attachment-spring-boot-starter` | → [wez-attachment.md](wez-attachment.md) |
| `wez-report` + 4 引擎 | `common-report-spring-boot-starter` | → [wez-report.md](wez-report.md) |

## 原生取代（2 個）

| Grails 模組 | 原生替代方案 | 詳細 |
|---|---|---|
| `wez-cache-redis` | `spring-boot-starter-data-redis` + `@EnableCaching` | → [wez-cache-redis.md](wez-cache-redis.md) |
| `wez-email` | `spring-boot-starter-mail` + `JavaMailSender` | → [wez-email.md](wez-email.md) |

## 待建 — 容易（1 個）

| Grails 模組 | 預計 Starter | 工作量 | 詳細 |
|---|---|---|---|
| `wez-crypto` | `common-crypto-starter` | 1-2 天 | → [wez-crypto.md](wez-crypto.md) |

## 待建 — 不容易（8 個）

| Grails 模組 | 預計 Starter | 工作量 | 難點 |
|---|---|---|---|
| `wez-ex-query` | `common-ex-query-starter` | 3-5 天 | REST 改造、jsqlparser、跨 DB |
| `wez-api-hub` | `common-api-hub-starter` | 3-5 天 | JWT 閘道、IP 白名單 |
| `wez-diagram-sign` | `common-signature-starter` | 3 天 | 依賴 attachment，前後端分離 |
| `wez-security-auth-gca` | `auth-gca` | 3 天 | 政府憑證，依賴 crypto |
| `wez-security-auth-moeaca` | `auth-moeaca` | 3 天 | 工商憑證 |
| `wez-security-auth-xca` | `auth-xca` | 3 天 | 組織憑證 |
| `wez-security-auth-health-card` | `auth-health-card` | 3 天 | 健保卡，硬體整合 |

## 不用做（14 個）

### 前後端分離淘汰（3 個）

| 模組 | 理由 |
|---|---|
| `wez-theme-bootstrap` | CSS/JS 資源，React 自己管 |
| `wez-theme-webix` | Webix UI 資源，不再使用 |
| `wez-web-bootstrap` | GSP + TagLib，前後端分離淘汰 |

### 太專案化（9 個）

| 模組 | 理由 |
|---|---|
| `wez-board` | 公告系統，綁定 security + attachment |
| `wez-notification` | 耦合 Sauser 安全模型 |
| `wez-log-viewer` | 綁定專屬 table（Bs400/Bs500/Bs800） |
| `wez-portal` | 系統入口，React 取代 |
| `wez-system` | 系統管理 CRUD，專案各異 |
| `wez-system-moica` | 自然人憑證管理頁面 |
| `wez-report-bug` | Bug 回報頁面，用 Sentry 取代 |
| `wez-diagram-family` | 家族樹圖，太特定領域 |
| `wez-web-api-hub` | API Hub 管理介面，前端取代 |

## 獨立運作（2 個）

| 模組 | 理由 |
|---|---|
| `wez-test` | Demo/測試用 |
| `websocket-proxy` | 已是 Spring Boot 3.1.2 |

## 建議開發順序

```
Phase 1（約 1 週）
  common-crypto-starter        ← wez-crypto

Phase 2（約 2 週）
  common-report-starter        ← wez-report + 引擎

Phase 3（看需求）
  common-ex-query-starter      ← wez-ex-query
  common-api-hub-starter       ← wez-api-hub
  common-signature-starter     ← wez-diagram-sign

Phase 4（care-security 擴充）
  auth-gca / auth-moeaca / auth-xca / auth-health-card
```
