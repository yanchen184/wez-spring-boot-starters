# {模組名稱}

{一句話描述} — {功能關鍵字1}、{功能關鍵字2}、{功能關鍵字3}

---

## 功能總覽

- **{功能 A}** — {簡短說明}
- **{功能 B}** — {簡短說明}
- **零配置** — 引入依賴即自動生效

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>{artifactId}</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. {最小使用範例}

```java
// 最簡單的使用方式，讓讀者 30 秒內能跑起來
```

### 3. 完成

不需要任何配置。{核心功能}自動生效。

---

## {核心概念 / API 說明}

{用表格或程式碼範例說明核心 API、欄位、方法}

| 方法 / 欄位 | 型態 | 說明 |
|-------------|------|------|
| ... | ... | ... |

---

## 配置

### application.yml

```yaml
common:
  {module}:
    enabled: true            # 是否啟用（預設 true）
    # ... 其他可選配置
```

> 如果模組是零配置（無 @ConfigurationProperties），此區塊改為：
>
> 本模組零配置。如需自訂行為，請參考「自訂 / 覆蓋」章節。

---

## 自訂 / 覆蓋

{說明消費端如何覆蓋預設行為，例如自訂 Bean、@Override、@AttributeOverride 等}

```java
// 覆蓋範例
```

---

## 進階用法

{Opt-in 功能、進階 API、邊緣情境的使用方式}

---

## 設計決策

### 要什麼

| 功能 | 原因 |
|------|------|
| ... | ... |

### 不要什麼

| 功能 | 原因 |
|------|------|
| ... | ... |

### 設計原則

1. **預設最小化** — 只做必要的事，進階功能 opt-in
2. **不過度設計** — 大部分場景用最簡單的方式就夠了
3. **可覆蓋** — 預設行為可被消費端替換
4. **不衝突** — 條件式自動配置，不與消費端打架

---

## 專案結構

```
{artifactId}/
├── src/main/java/com/company/common/{module}/
│   ├── config/
│   │   └── {Module}AutoConfiguration.java
│   ├── ...
│   └── ...
└── pom.xml
```

---

## 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 自動配置 | `AutoConfiguration.imports`（Spring Boot 3.0+） |

---

## 版本

- 1.0.0
  - {初始功能清單}
