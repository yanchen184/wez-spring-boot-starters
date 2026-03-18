# wez-grails5 → Spring Boot Starter 遷移分析

> 分析日期：2026-03-17
> 分析範圍：42 個 Grails 模組

---

## 1. 已經做過（有非常類似的 Spring Boot 對應）

| Grails 模組 | Spring Boot 對應 | 完成度 |
|---|---|---|
| `wez-base-core`（Sauser, Role, Org, Menu, Perm） | `care-security` core | ✅ 完成 |
| `wez-base`（基礎功能） | `common-jpa-starter` | ✅ 完成 |
| `wez-security`（認證/權限） | `care-security` | ✅ 完成 |
| `wez-web-security`（密碼變更/重設） | `care-security` AuthService | ✅ 完成 |
| `wez-logs` + `wez-logs-extra-filter` | `common-log-starter` | ✅ 完成 |
| `wez-captcha` + `wez-security-captcha` | `auth-captcha` | ✅ 完成 |
| `wez-security-auth-otp` | `auth-otp` | ✅ 完成 |
| `wez-security-auth-moica-*`（2 個） | `auth-moica` | 🔶 大致完成 |
| 統一回應/例外處理 | `common-response-starter` | ✅ 完成 |

**小計：9 個已遷移（含 2 個 plugin + 7 個 backend）**

---

## 2. 適合做成 Starter

### 容易的（1-2 天/個）

| 模組 | 建議 Starter | 原始碼量 | 理由 |
|---|---|---|---|
| `wez-crypto` | `common-crypto-starter` | 12 檔 | 純 Java 工具類，零 Grails 耦合，含台灣憑證（MOICA/GCA/XCA）處理，直接搬 |
| `wez-email` | `common-email-starter` | 6 檔 | 核心只有 31 行，包裝 Spring Mail + 寄件紀錄（Bs500） |
| `wez-cache-redis` | `common-cache-redis-starter` | 10 檔 | 薄包裝 Spring Data Redis + Gson 序列化，幾乎不用改 |

### 不容易的（3-7 天/個）

| 模組 | 建議 Starter | 原始碼量 | 難點 |
|---|---|---|---|
| `wez-attachment` | `common-attachment-starter` | 12 檔 | 檔案系統/DB Blob 雙模式、Apache Tika 偵測、多格式預覽邏輯 |
| `wez-report` + 4 個引擎 | `common-report-starter` | 50+ 檔 | 5 個模組整合、JODConverter 轉檔、多引擎插拔架構（EasyExcel/iReport/xDocReport/Export） |
| `wez-ex-query` | `common-ex-query-starter` | 6 檔 | Controller 要改 REST、jsqlparser SQL 解析、跨 DB 支援（MSSQL/Oracle/PG/MySQL） |
| `wez-api-hub` | `common-api-hub-starter` | 14 檔 | JWT 閘道、IP 白名單、使用者/權限管理、JJWT 整合 |
| `wez-diagram-sign` | `common-signature-starter` | 6 檔 | 依賴 attachment starter，Canvas 簽名前後端分離要重設計 |
| `wez-security-auth-gca` | `auth-gca`（care-security 子模組） | — | 政府憑證 PKI 驗證，需要 crypto starter |
| `wez-security-auth-moeaca` | `auth-moeaca`（care-security 子模組） | — | 工商憑證，類似 moica 架構 |
| `wez-security-auth-xca` | `auth-xca`（care-security 子模組） | — | 組織/團體憑證，類似 moica 架構 |
| `wez-security-auth-health-card` | `auth-health-card`（care-security 子模組） | — | 健保卡讀取，硬體整合 |

---

## 3. 不用做的

### 前後端分離後不需要（3 個）

| 模組 | 理由 |
|---|---|
| `wez-theme-bootstrap` | CSS/JS 資源，React 專案自己管 |
| `wez-theme-webix` | Webix UI 框架資源，不再使用 |
| `wez-web-bootstrap` | GSP 視圖 + TagLib，前後端分離淘汰 |

### 太專案化，不通用（9 個）

| 模組 | 理由 |
|---|---|
| `wez-board` | 公告系統，業務邏輯綁定 security + attachment，每個專案需求不同 |
| `wez-notification` | 通知系統，耦合 Sauser 安全模型，要用需大幅重構 |
| `wez-log-viewer` | 綁定公司專屬 table（Bs400/Bs500/Bs800），不通用 |
| `wez-portal` | 系統入口/儀表板，前端 React 取代 |
| `wez-system` | 系統管理 CRUD，每個專案不同 |
| `wez-system-moica` | 自然人憑證管理頁面，前端取代 |
| `wez-report-bug` | Bug 回報頁面，前端取代或用第三方（Sentry） |
| `wez-diagram-family` | 家族樹圖，太特定領域 |
| `wez-web-api-hub` | API Hub 管理**介面**，前端取代（邏輯在 plugin/wez-api-hub） |

### 獨立運作（2 個）

| 模組 | 理由 |
|---|---|
| `wez-test` | Demo/測試用，不需遷移 |
| `websocket-proxy` | 已經是 Spring Boot 3.1.2，獨立運作中 |

---

## 總結

| 分類 | 數量 |
|---|---|
| ✅ 已完成 | 11 個 |
| 🟢 適合做 Starter（容易） | 1 個 |
| 🟡 適合做 Starter（不容易） | 7 個 |
| ⬜ 不用做 | 14 個 |
| 🔵 獨立運作 | 2 個 |
| **合計** | **37 個**（另有 5 個 report 子模組合併算 1 個） |

---

## 建議開發順序

```
Phase 1（最快出成果，約 1 週）
  common-crypto-starter        ← wez-crypto，1-2 天
  common-email-starter         ← wez-email，1-2 天
  common-cache-redis-starter   ← wez-cache-redis，1 天

Phase 2（中等工作量，約 2 週）
  common-attachment-starter    ← wez-attachment，3-5 天
  common-report-starter        ← wez-report + 引擎，5-7 天

Phase 3（看需求）
  common-ex-query-starter      ← wez-ex-query
  common-api-hub-starter       ← wez-api-hub
  common-signature-starter     ← wez-diagram-sign（依賴 attachment）

Phase 4（care-security 擴充）
  auth-gca                     ← wez-security-auth-gca
  auth-moeaca                  ← wez-security-auth-moeaca
  auth-xca                     ← wez-security-auth-xca
  auth-health-card             ← wez-security-auth-health-card
```

---

## 依賴關係

```
獨立（無依賴）
  common-crypto-starter
  common-email-starter
  common-cache-redis-starter
  common-ex-query-starter

有依賴
  common-attachment-starter
    └─ common-signature-starter（依賴 attachment）

  common-report-starter（基礎框架）
    ├─ report-easyexcel（Excel）
    ├─ report-ireport（JasperReports）
    ├─ report-xdocreport（Word/ODT）
    └─ report-export（CSV/ODS/XLSX）

  care-security（已完成）
    ├─ auth-gca（依賴 crypto）
    ├─ auth-moeaca（依賴 crypto）
    ├─ auth-xca（依賴 crypto）
    └─ auth-health-card
```
