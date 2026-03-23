# Common Notification Spring Boot Starter

多通道通知系統 — 郵件、WebSocket 即時推播、排程發送、失敗重試、狀態追蹤

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
| 寄信 | 自己寫 JavaMailSender | `notificationService.send()` 一行搞定 |
| 寄失敗 | 沒了就沒了 | 自動重試（可配置次數） |
| 狀態追蹤 | 無 | PENDING → SENDING → SENT / FAILED，所有紀錄持久化到 DB |
| 排程發送 | 無 | 指定 `sendAt` 延遲/定時發送 |
| 通道擴充 | switch 硬編碼 | SPI 可插拔，實作 `NotificationChannel` 即可 |
| 範本 | 直接傳 HTML | Thymeleaf 範本引擎，內容和格式分離 |
| 非同步 | `new Thread()` | 獨立線程池 + @Async |
| 清理 | 手動清理 | 自動清理過期紀錄（預設 90 天） |

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-notification-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 實作 RecipientResolver（必須）

```java
@Component
public class UserRecipientResolver implements RecipientResolver {

    private final UserRepository userRepository;

    @Override
    public String resolveEmail(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail).orElse(null);
    }

    @Override
    public String resolveDisplayName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getDisplayName).orElse("User#" + userId);
    }
}
```

### 3. 設定 application.yml

```yaml
common:
  notification:
    enabled: true
    from-address: noreply@company.com

spring:
  mail:
    host: smtp.company.com
    port: 587
    username: xxx
    password: xxx
```

### 4. 發送通知

```java
notificationService.send(NotificationMessage.builder()
        .to(userId)
        .subject("審核通過通知")
        .template("approval-passed")
        .data("itemName", itemName)
        .channels("EMAIL", "WEBSOCKET")
        .category("APPROVAL")
        .build());
```

### 5. 完成

---

## 功能總覽

- **多通道 SPI** — EMAIL / WEBSOCKET / 自訂（實作 `NotificationChannel` 即可擴充）
- **排程發送** — 指定 `sendAt` 延遲/定時發送，排程器自動處理
- **失敗重試** — 可配置最大重試次數（預設 3 次），排程器自動掃描重試
- **狀態追蹤** — `PENDING → SENDING → SENT / FAILED`，所有紀錄持久化到 DB
- **範本引擎** — Thymeleaf 郵件範本，通知內容和格式分離
- **非同步執行** — 獨立線程池，不阻塞業務流程
- **自動清理** — 定時清理過期紀錄（預設 90 天）

---

## 核心 API

### NotificationService

```java
// 即時發送
notificationService.send(NotificationMessage.builder()
        .to(userId)
        .subject("審核通過通知")
        .template("approval-passed")
        .data("itemName", itemName)
        .data("approvedAt", LocalDateTime.now().toString())
        .channels("EMAIL", "WEBSOCKET")
        .category("APPROVAL")
        .build());

// 排程發送（明天早上 9 點）
notificationService.schedule(NotificationMessage.builder()
        .to(userId)
        .subject("待辦提醒")
        .content("<p>您有待處理的審核項目</p>")
        .channels("EMAIL")
        .sendAt(Instant.now().plus(Duration.ofHours(12)))
        .build());
```

### NotificationMessage Builder

| 方法 | 說明 |
|------|------|
| `.to(userId)` | 收件人 ID |
| `.subject(subject)` | 主旨 |
| `.template(name)` | Thymeleaf 範本名稱（放在 `templates/notification/` 下） |
| `.content(html)` | 直接傳 HTML 內容（不用範本時） |
| `.data(key, value)` | 範本變數 |
| `.channels("EMAIL", "WEBSOCKET")` | 發送通道（可多個） |
| `.category(category)` | 分類（SYSTEM / APPROVAL / ...） |
| `.sendAt(instant)` | 排程發送時間 |

### 自訂通道（SPI 可插拔）

通知系統不綁死特定通道。內建 EMAIL + WEBSOCKET，但你可以隨時加新通道 — **不需要改 starter 原始碼**，只要在消費端實作 `NotificationChannel` 介面並註冊為 Spring Bean，就自動生效。

#### 範例一：SMS 簡訊通道

```java
@Component
public class SmsChannel implements NotificationChannel {

    private final SmsClient smsClient;
    private final UserRepository userRepository;

    @Override
    public String getChannelName() { return "SMS"; }

    @Override
    public void send(NotificationLog notification) throws Exception {
        String phone = userRepository.findById(notification.getRecipientId())
                .map(User::getPhone).orElseThrow();
        smsClient.send(phone, notification.getContent());
    }
}
```

#### 範例二：LINE Notify 通道

```java
@Component
public class LineNotifyChannel implements NotificationChannel {

    private final RestClient restClient;

    @Override
    public String getChannelName() { return "LINE"; }

    @Override
    public void send(NotificationLog notification) throws Exception {
        restClient.post()
                .uri("https://notify-api.line.me/api/notify")
                .header("Authorization", "Bearer " + lineToken)
                .body("message=" + notification.getSubject())
                .retrieve().toBodilessEntity();
    }
}
```

#### 使用

加入 Bean 後自動註冊，發通知時指定通道名稱即可：

```java
// 走 SMS
notificationService.send(message.channels("SMS").build());

// 走 LINE
notificationService.send(message.channels("LINE").build());

// 同時走 EMAIL + LINE
notificationService.send(message.channels("EMAIL", "LINE").build());
```

### 自訂郵件範本

在 `src/main/resources/templates/notification/` 下建立 Thymeleaf 範本：

```html
<!-- templates/notification/approval-passed.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
  <h2>審核通過</h2>
  <p>您的 <strong th:text="${itemName}">項目</strong> 已通過審核。</p>
  <p>審核時間：<span th:text="${approvedAt}">2026-03-19</span></p>
</body>
</html>
```

### NotificationScheduler 排程器

```
NotificationScheduler
├── processScheduled() — 每 30 秒掃描待發送通知
├── retryFailed()      — 每 5 分鐘重試失敗通知
└── cleanup()          — 每天凌晨 3 點清理過期紀錄
```

### 資料庫表

自動建立 `NOTIFICATION_LOG` 表：

| 欄位 | 說明 |
|------|------|
| OBJID | PK |
| RECIPIENT_ID | 收件人 ID |
| CHANNEL | 通道（EMAIL / WEBSOCKET / ...） |
| CATEGORY | 分類（SYSTEM / APPROVAL / ...） |
| SUBJECT | 主旨 |
| CONTENT | 內容（HTML） |
| STATUS | 狀態（PENDING / SENDING / SENT / FAILED） |
| SEND_AT | 排程發送時間 |
| SENT_AT | 實際發送時間 |
| ERROR_MESSAGE | 錯誤訊息 |
| RETRY_COUNT | 重試次數 |
| VERSION | 樂觀鎖 |

---

## 配置

### application.yml

```yaml
common:
  notification:
    enabled: true                  # 是否啟用（預設 true）
    from-address: noreply@company.com  # 寄件人地址（必填）
    default-channels:              # 預設通道（預設 [EMAIL]）
      - EMAIL
    max-retry: 3                   # 最大重試次數（預設 3）
    retention-days: 90             # 紀錄保留天數（預設 90）
    async:
      core-pool-size: 2            # 線程池核心數（預設 2）
      max-pool-size: 5             # 線程池最大數（預設 5）
      queue-capacity: 100          # 任務佇列容量（預設 100）

# 郵件設定（Spring Boot 原生）
spring:
  mail:
    host: smtp.company.com         # SMTP 伺服器
    port: 587                      # SMTP 埠號
    username: xxx                  # 帳號
    password: xxx                  # 密碼
```

---

## 設計決策

### 要什麼

| 功能 | 原因 |
|------|------|
| SPI 可插拔通道 | 不同專案需要不同通道（EMAIL / LINE / SMS），不應硬編碼 |
| 狀態追蹤 + 持久化 | 所有通知紀錄可查詢、可追蹤、可重試 |
| 排程發送 | 支援延遲/定時發送，如「明天早上 9 點提醒」 |
| 失敗重試 | 網路抖動、SMTP 暫時不可用等場景，自動重試 |
| Thymeleaf 範本 | 內容與格式分離，維護方便 |
| 獨立線程池 | 不阻塞業務流程，不影響主線程池 |
| 自動清理 | 避免通知表無限增長 |

### 不要什麼

| 功能 | 原因 |
|------|------|
| 綁死特定通道 | 每個專案需求不同，SPI 可插拔最靈活 |
| 同步發送 | 寄信/推播不應阻塞業務流程 |
| 無限重試 | 避免永遠失敗的通知占用資源 |
| 不持久化 | 無法追蹤、無法重試、無法查詢歷史 |

### 設計原則

1. **可插拔** — 通道透過 SPI 擴充，不需要改 starter 原始碼
2. **可靠** — 所有通知持久化、失敗自動重試、狀態可追蹤
3. **非阻塞** — 獨立線程池，不影響業務流程
4. **可維護** — Thymeleaf 範本引擎，內容與格式分離

---

## 依賴關係

```
common-notification-spring-boot-starter
├── common-jpa-spring-boot-starter (Entity 審計、JPA)
├── spring-boot-starter-data-jpa
├── spring-boot-starter-mail (JavaMailSender)
├── spring-boot-starter-thymeleaf (範本引擎)
└── spring-boot-starter-websocket (optional, WebSocket 通道)
```

---

## 專案結構與技術規格

### 目錄樹

```
common-notification-spring-boot-starter/
├── pom.xml
│
├── src/main/java/com/company/common/notification/
│   ├── channel/
│   │   ├── NotificationChannel.java          # SPI 介面：getChannelName() + send()
│   │   ├── RecipientResolver.java            # 收件人解析介面（消費端必須實作）
│   │   ├── EmailChannel.java                 # 郵件通道（JavaMailSender + Thymeleaf）
│   │   └── WebSocketChannel.java             # WebSocket 通道（SimpMessagingTemplate）
│   ├── config/
│   │   ├── NotificationAutoConfiguration.java # 自動配置
│   │   └── NotificationProperties.java        # 配置屬性（common.notification.*）
│   ├── dto/
│   │   └── NotificationMessage.java           # 通知訊息 Builder
│   ├── entity/
│   │   ├── NotificationLog.java               # 通知紀錄 Entity
│   │   └── NotificationStatus.java            # 狀態 enum（PENDING/SENDING/SENT/FAILED）
│   ├── repository/
│   │   └── NotificationLogRepository.java     # JPA Repository
│   └── service/
│       ├── NotificationService.java           # 主 facade：send() / schedule() / renderContent()
│       └── NotificationScheduler.java         # 排程器：processScheduled / retryFailed / cleanup
│
├── src/main/resources/META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── src/main/resources/templates/notification/  # 消費端放 Thymeleaf 範本的位置
```

### 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 自動配置 | `AutoConfiguration.imports` |
| 郵件 | Spring Boot Starter Mail（JavaMailSender） |
| 範本引擎 | Thymeleaf |
| WebSocket | STOMP（SimpMessagingTemplate），optional |
| 排程 | `@Scheduled`（processScheduled / retryFailed / cleanup） |
| 非同步 | 獨立 `ThreadPoolTaskExecutor` |
| 依賴 | `common-jpa-spring-boot-starter` |

### vs Grails 版（wez-notification + wez-email）

| 功能 | Grails | Spring Boot |
|------|--------|-------------|
| 寄失敗 | 沒了就沒了 | 自動重試（可配置次數） |
| 狀態追蹤 | 無 | PENDING → SENT / FAILED |
| 排程發送 | 無 | 支援 sendAt 延遲/定時 |
| 通道擴充 | switch 硬編碼 | SPI 可插拔 |
| 範本 | 直接傳 HTML | Thymeleaf 範本引擎 |
| 非同步 | new Thread() | 線程池 + @Async |
| 清理 | 無 | 自動清理過期紀錄 |
| 批次效率 | 一封一封寄 | 線程池並行處理 |

---

## 版本

### 1.0.0

- 多通道 SPI 架構（EMAIL + WEBSOCKET + 自訂）
- NotificationService（即時發送 + 排程發送）
- 失敗重試（可配置最大次數）
- 狀態追蹤（PENDING → SENDING → SENT / FAILED）
- Thymeleaf 郵件範本引擎
- NotificationScheduler（排程發送 + 重試 + 自動清理）
- 獨立線程池，非阻塞
- RecipientResolver 介面（消費端實作）
