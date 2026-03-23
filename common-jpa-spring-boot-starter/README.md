# Common JPA Spring Boot Starter

JPA 通用模組 — 自動審計、可選軟刪除、預設 AuditorAware

---

## 目錄

- [加入後你的專案自動獲得](#加入後你的專案自動獲得)
- [快速開始](#快速開始)
- [功能總覽](#功能總覽)
- [核心 API](#核心-api)
- [配置](#配置)
- [設計決策](#設計決策)
- [依賴關係](#依賴關係)
- [專案結構與技術規格](#專案結構與技術規格)
- [版本](#版本)

---

## 加入後你的專案自動獲得

| 功能 | 加入前 | 加入後 |
|------|--------|--------|
| 審計欄位 | 需要每個 Entity 手動加 createdDate 等 4 個欄位 | 繼承 `AuditableEntity` 自動獲得 |
| 審計人 | 需要手動塞 `createdBy` / `lastModifiedBy` | 自動從 SecurityContext 取得當前使用者 |
| 軟刪除 | 需要自己寫 `deleted` 欄位 + 自訂 query | 繼承 `BaseEntity` + `SoftDeleteRepository` 即有完整 API |
| 配置 | 需要自己加 `@EnableJpaAuditing` | 零配置，引入依賴即生效 |

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-jpa-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 定義實體

```java
@Entity
@Table(name = "PRODUCT")
@Getter @Setter
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal price;
}
```

### 3. 定義 Repository

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

### 4. 完成

不需要任何配置。審計欄位自動填入。

---

## 功能總覽

- **自動審計** — 所有繼承 `AuditableEntity` 的實體自動記錄建立/修改時間與操作人
- **AuditorAware** — 從 Spring Security SecurityContext 自動取得當前使用者
- **可選軟刪除** — 需要時才繼承 `BaseEntity`，搭配 `SoftDeleteRepository`
- **零配置** — 引入依賴即自動生效

---

## 核心 API

### 審計欄位

繼承 `AuditableEntity` 後自動獲得：

| Java 欄位 | DB 欄位 | 型態 | 說明 |
|-----------|---------|------|------|
| `createdDate` | `created_date` | `LocalDateTime` | 建立時間（不可更新） |
| `lastModifiedDate` | `last_modified_date` | `LocalDateTime` | 最後修改時間 |
| `createdBy` | `created_by` | `String` | 建立人（不可更新） |
| `lastModifiedBy` | `last_modified_by` | `String` | 最後修改人 |

審計人來源：`SecurityContextHolder.getContext().getAuthentication().getName()`

無認證時（排程、系統初始化）：回傳 `"SYSTEM"`

### Entity 繼承結構

```
AuditableEntity                       <-- 大部分實體用這個
|-- createdDate (LocalDateTime)
|-- lastModifiedDate (LocalDateTime)
|-- createdBy (String)
|-- lastModifiedBy (String)

BaseEntity extends AuditableEntity    <-- 真的需要軟刪除才用
|-- (繼承 4 個審計欄位)
|-- deleted (boolean)
|-- version (Integer, @Version)
```

選擇依據：

| 場景 | 繼承 | Repository |
|------|------|------------|
| 一般 CRUD | `AuditableEntity` | `JpaRepository` |
| 需要軟刪除 | `BaseEntity` | `SoftDeleteRepository` |

### SoftDeleteRepository API

| 方法 | 回傳 | 說明 |
|------|------|------|
| `findAllActive()` | `List<T>` | 查詢未刪除 |
| `findAllActive(Pageable)` | `Page<T>` | 分頁查詢未刪除 |
| `findByIdActive(id)` | `Optional<T>` | 按 ID 查詢未刪除 |
| `findAllByIdActive(ids)` | `List<T>` | 批次查詢未刪除 |
| `softDeleteById(id)` | `int` | 邏輯刪除 |
| `softDeleteByIds(ids)` | `int` | 批次邏輯刪除 |
| `restoreById(id)` | `int` | 恢復刪除 |
| `countActive()` | `long` | 統計未刪除數量 |
| `existsByIdActive(id)` | `boolean` | 是否存在且未刪除 |

### 軟刪除用法

```java
@Entity
public class ImportantRecord extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
}

public interface ImportantRecordRepository
        extends SoftDeleteRepository<ImportantRecord, Long> {
}
```

### 自訂 AuditorAware

預設的 `DefaultAuditorAware` 從 SecurityContext 取使用者名稱。消費端可覆蓋：

```java
@Bean
public AuditorAware<String> auditorAware() {
    return () -> Optional.of("custom-user");
}
```

定義後預設 Bean 自動停用（`@ConditionalOnMissingBean`）。

### 欄位名映射

DB 欄位名與預設不同時，用 `@AttributeOverride`：

```java
@Entity
@AttributeOverride(name = "createdDate",      column = @Column(name = "DATE_CREATED"))
@AttributeOverride(name = "lastModifiedDate",  column = @Column(name = "LAST_UPDATED"))
@AttributeOverride(name = "createdBy",         column = @Column(name = "CREATED_BY"))
@AttributeOverride(name = "lastModifiedBy",    column = @Column(name = "LAST_UPDATED_BY"))
public class LegacyTable extends AuditableEntity {
    // 映射到既有的舊 DB 欄位名
}
```

---

## 配置

零配置，引入即生效。

不需要任何 `application.yml` 設定。`@EnableJpaAuditing` 由 starter 條件式自動啟用，不與消費端的設定衝突。

---

## 設計決策

### 要什麼

| 功能 | 原因 |
|------|------|
| 4 個審計欄位 | 所有業務表都需要知道誰在什麼時候改了什麼 |
| 從 SecurityContext 自動取審計人 | 不需要手動塞，減少遺漏 |
| 可覆蓋的 AuditorAware | 消費端定義自己的 Bean 即可替換 |
| 條件式 @EnableJpaAuditing | 不與消費端的設定衝突 |

### 不要什麼

| 功能 | 原因 |
|------|------|
| 預設軟刪除 | 關聯表、日誌表、歷史表都不需要；每個 query 要加 `WHERE deleted=false` 容易漏 |
| 預設樂觀鎖 | 管理後台 CRUD 很少並發衝突；`OptimisticLockException` 對使用者不友善 |
| 內建 @Id | 主鍵策略（IDENTITY / UUID / SEQUENCE）由消費端決定 |
| @ConfigurationProperties | 審計功能不需要配置，加了只是增加複雜度 |
| @SQLRestriction 全局過濾 | 過度魔法，讓開發者明確選擇 `findAllActive()` 比隱式過濾更安全 |

### 設計原則

1. **預設最小化** — 只加審計，軟刪除/樂觀鎖要明確 opt-in
2. **不過度設計** — 大部分表只要 `AuditableEntity` + `JpaRepository` 就夠了
3. **可覆蓋** — AuditorAware、JpaAuditing 都可以被消費端替換
4. **不衝突** — 條件式自動配置，不會跟消費端的 `@EnableJpaAuditing` 打架

---

## 依賴關係

```
common-jpa-spring-boot-starter
├── spring-boot-starter-data-jpa (provided)
└── spring-security-core (provided, 用於 AuditorAware)
```

---

## 專案結構與技術規格

### 目錄樹

```
common-jpa-spring-boot-starter/
├── src/main/java/com/company/common/jpa/
|   ├── auditor/
|   |   └── DefaultAuditorAware.java       # 預設審計人（SecurityContext）
|   ├── config/
|   |   └── JpaAutoConfiguration.java      # 自動配置（條件式 @EnableJpaAuditing）
|   ├── entity/
|   |   ├── AuditableEntity.java           # 審計基礎實體（推薦）
|   |   └── BaseEntity.java               # 審計 + 軟刪除 + 樂觀鎖
|   └── repository/
|       ├── SoftDeleteRepository.java      # 軟刪除 Repository
|       └── BaseRepository.java            # @Deprecated, 向後相容
└── pom.xml
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 自動配置 | `AutoConfiguration.imports`（Spring Boot 3.0+） |
| 審計實作 | Spring Data JPA `@EnableJpaAuditing`（條件式） |
| 審計人來源 | `SecurityContextHolder`（可覆蓋） |
| 無認證時 | `"SYSTEM"` |

---

## 版本

### 1.0.0

- AuditableEntity（4 審計欄位）
- BaseEntity（審計 + 軟刪除 + 樂觀鎖，opt-in）
- SoftDeleteRepository（含分頁）
- DefaultAuditorAware（SecurityContext）
- 條件式 @EnableJpaAuditing（不與消費端衝突）
