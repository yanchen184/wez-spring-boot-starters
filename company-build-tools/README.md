# Company Build Tools

公司統一 Build 規則 — Checkstyle 程式碼風格 + SpotBugs 靜態分析，所有後端專案共用。

---

## 功能總覽

- **Checkstyle** — Google Java Style Guide 精簡版，擋真正有害的問題，不擋風格偏好
- **SpotBugs** — 靜態分析 + 已知誤報過濾，避免框架慣例造成的假警報
- 由 root POM 自動引用，子模組零配置
- `mvn verify` 時自動執行，不需手動觸發

---

## 核心 API

此模組無 Java 類別，核心產出為兩個規則檔。

### Checkstyle 規則（`checkstyle.xml`）

#### Import 規範

| 規則 | 說明 |
|------|------|
| `AvoidStarImport` | 禁止 wildcard import（`import java.util.*`） |
| `UnusedImports` | 禁止 unused import |
| `RedundantImport` | 禁止重複 import |
| `IllegalImport` | 禁止 `java.lang.*` 顯式 import |

#### 命名規範

| 規則 | 說明 |
|------|------|
| `TypeName` | Class 名必須大駝峰 |
| `MethodName` | 方法名必須小駝峰 |
| `LocalVariableName` / `MemberName` / `ParameterName` | 變數名必須小駝峰 |
| `ConstantName` | 全大寫 + 底線，允許 `log`/`logger` 及小駝峰工具物件 |
| `StaticVariableName` | static non-final 欄位必須小駝峰 |

#### 程式碼品質

| 規則 | 說明 |
|------|------|
| `EmptyCatchBlock` | 禁止空 catch（變數名為 `ignored`/`expected` 時例外） |
| `EmptyBlock` | 禁止空的 if/else/try/finally |
| `EmptyStatement` | 禁止多餘分號 |
| `MissingSwitchDefault` | switch 必須有 default |
| `FallThrough` | case fall-through 必須有 `// fall through` 註解 |
| `StringLiteralEquality` | 禁止字串用 `==` 比較 |
| `ModifiedControlVariable` | 禁止修改 for-loop 控制變數 |
| `MethodLength` | 方法上限 80 行（不含空行） |
| `ParameterNumber` | 方法參數上限 7 個（忽略 `@Override`、`@Bean`、Constructor） |

#### 格式與空白

| 規則 | 說明 |
|------|------|
| `LineLength` | 行寬上限 150 字元（忽略 import/package/URL） |
| `FileTabCharacter` | 禁止 Tab，統一用空格 |
| `NewlineAtEndOfFile` | 檔案結尾必須有換行 |
| `NeedBraces` | if/for/while 必須有大括號 |
| `RightCurly` | 右大括號位置規範 |
| `WhitespaceAround` | 操作符前後要有空格（允許空 block `{}`） |
| `WhitespaceAfter` | 逗號後要有空格 |
| Trailing whitespace | 不能有行尾空白 |

#### 刻意不加的規則

| 規則 | 原因 |
|------|------|
| `UnnecessaryParentheses` | Lambda 括號是偏好問題，Security DSL method chain 常被誤報 |
| `LeftCurly` | Spring 的 one-liner `interface extends JpaRepository<X,Long> {}` 會大量誤報 |
| Javadoc 強制要求 | 精簡版不強制，避免為寫而寫 |

#### 壓制方式

```java
@SuppressWarnings("checkstyle:MethodLength")
public void longMethod() { ... }
```

### SpotBugs 排除規則（`spotbugs-exclude.xml`）

| 排除對象 | Bug Pattern | 原因 |
|----------|-------------|------|
| `*AutoConfiguration` | `UWF_UNWRITTEN_FIELD`, `NP_UNWRITTEN_FIELD` | Spring `@Autowired`/`@Value` 框架注入 |
| JPA Entity (`*.entity.*`) | `UWF_UNWRITTEN_FIELD`, `NP_UNWRITTEN_FIELD`, `SE_NO_SERIALVERSIONID` | Hibernate 管理欄位 |
| DTO/Record (`*Dto`, `*Request`, `*Response`) | `EQ_UNUSUAL` | equals/hashCode 由編譯器生成 |
| 測試程式碼 (`*Test*`) | `DM_DEFAULT_ENCODING`, `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` | 測試允許寬鬆規則 |
| `*SecurityConfig*` | `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT` | HttpSecurity DSL method chain 誤報 |
| `TotpService` | `DMI_RANDOM_USED_ONLY_ONCE` | SecureRandom 單次使用是刻意設計 |

---

## 配置

此模組由 root POM 的 plugin 配置自動引用，不需要額外 YAML 配置。

```xml
<!-- root pom.xml 已配置（不需手動加） -->
<checkstyle.config>checkstyle.xml</checkstyle.config>
<spotbugs.excludeFilter>spotbugs-exclude.xml</spotbugs.excludeFilter>
```

### 常用命令

```bash
# 執行完整靜態分析（Checkstyle + SpotBugs）
mvn verify -DskipTests

# 只跑 Checkstyle
mvn checkstyle:check

# 只跑 SpotBugs
mvn spotbugs:check
```

### 自訂規則

編輯 `src/main/resources/` 下的規則檔，所有子模組下次 build 自動生效。

---

## 設計決策

| 要 | 不要 |
|----|------|
| 擋真正有害的問題 | 擋風格偏好 |
| Google Java Style 精簡版 | 完整 Google Style（太嚴格） |
| 框架慣例白名單 | 強制修改框架寫法 |
| 只排除已知誤報 | 排除真實問題 |
| 不繼承 root POM | 循環依賴（root plugin 依賴此 jar） |

---

## 依賴關係

```
company-build-tools（獨立模組，不繼承 root POM）
├── checkstyle.xml        ← Checkstyle 規則
└── spotbugs-exclude.xml  ← SpotBugs 排除規則

被引用方式：
root pom.xml
└── maven-checkstyle-plugin / spotbugs-maven-plugin
    └── company-build-tools (dependency)  ← 從 classpath 讀取規則檔
```

---

## 專案結構與技術規格

### 目錄樹

```
company-build-tools/
├── pom.xml
├── README.md
└── src/main/resources/
    ├── checkstyle.xml           # Checkstyle 規則（Google Style 精簡版）
    └── spotbugs-exclude.xml     # SpotBugs 排除過濾器
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Checkstyle Plugin | 3.6.0 |
| SpotBugs Plugin | 4.9.8.2 |
| 打包方式 | JAR（純資源檔，無 Java 程式碼） |
| 適用範圍 | 所有子模組（`mvn verify` 自動觸發） |

---

## 版本

- **1.0.0** — 初始版本：Checkstyle Google Style 精簡版 + SpotBugs 框架誤報過濾
