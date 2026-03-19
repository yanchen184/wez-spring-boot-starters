# common-notification-spring-boot-starter

多通道通知系統，支援郵件、WebSocket 即時推播、排程發送、失敗重試、狀態追蹤。

## 功能

- **多通道 SPI** — EMAIL / WEBSOCKET / 自訂（實作 `NotificationChannel` 即可擴充）
- **排程發送** — 指定 `sendAt` 延遲/定時發送，排程器自動處理
- **失敗重試** — 可配置最大重試次數（預設 3 次），排程器自動掃描重試
- **狀態追蹤** — `PENDING → SENDING → SENT / FAILED`，所有紀錄持久化到 DB
- **範本引擎** — Thymeleaf 郵件範本，通知內容和格式分離
- **非同步執行** — 獨立線程池，不阻塞業務流程
- **自動清理** — 定時清理過期紀錄（預設 90 天）

## 快速開始

### 1. 加入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-notification-spring-boot-starter</artifactId>
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

### 3. 發送通知

```java
@Service
public class ApprovalService {

    private final NotificationService notificationService;

    // 即時發送
    public void onApproved(Long userId, String itemName) {
        notificationService.send(NotificationMessage.builder()
                .to(userId)
                .subject("審核通過通知")
                .template("approval-passed")
                .data("itemName", itemName)
                .data("approvedAt", LocalDateTime.now().toString())
                .channels("EMAIL", "WEBSOCKET")
                .category("APPROVAL")
                .build());
    }

    // 排程發送（明天早上 9 點）
    public void scheduleReminder(Long userId) {
        notificationService.schedule(NotificationMessage.builder()
                .to(userId)
                .subject("待辦提醒")
                .content("<p>您有待處理的審核項目</p>")
                .channels("EMAIL")
                .sendAt(Instant.now().plus(Duration.ofHours(12)))
                .build());
    }
}
```

## 設定屬性

```yaml
common:
  notification:
    enabled: true
    from-address: noreply@company.com
    default-channels: [EMAIL]
    max-retry: 3
    retention-days: 90
    async:
      core-pool-size: 2
      max-pool-size: 5
      queue-capacity: 100

# 郵件（Spring Boot 原生）
spring:
  mail:
    host: smtp.company.com
    port: 587
    username: xxx
    password: xxx
```

## 自訂通道（SPI 可插拔）

通知系統不綁死特定通道。內建 EMAIL + WEBSOCKET，但你可以隨時加新通道 — **不需要改 starter 原始碼**，只要在消費端實作 `NotificationChannel` 介面並註冊為 Spring Bean，就自動生效。

### 範例一：SMS 簡訊通道

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

### 範例二：LINE Notify 通道

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

### 使用

加入 Bean 後自動註冊，發通知時指定通道名稱即可：

```java
// 走 SMS
notificationService.send(message.channels("SMS").build());

// 走 LINE
notificationService.send(message.channels("LINE").build());

// 同時走 EMAIL + LINE
notificationService.send(message.channels("EMAIL", "LINE").build());
```

## 自訂郵件範本

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

## 資料庫表

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

## 架構

```
NotificationService（入口）
├── send()      — 即時非同步發送
├── schedule()  — 排程發送（寫入 DB，等排程器處理）
└── renderContent() — Thymeleaf 範本渲染

NotificationScheduler（排程器）
├── processScheduled() — 每 30 秒掃描待發送通知
├── retryFailed()      — 每 5 分鐘重試失敗通知
└── cleanup()          — 每天凌晨 3 點清理過期紀錄

NotificationChannel（SPI）
├── EmailChannel      — JavaMailSender + Thymeleaf
├── WebSocketChannel  — SimpMessagingTemplate (STOMP)
└── 自訂 Channel...   — 消費端擴充
```

## vs Grails 版（wez-notification + wez-email）

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
