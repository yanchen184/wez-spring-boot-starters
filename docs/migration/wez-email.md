# wez-email → 原生取代

> 狀態：✅ 原生取代（不做 Starter）

## 決策理由

- `WezEmailService` 核心只有 31 行，僅包裝 Grails Mail Plugin 的 `sendMail` DSL
- `WezBs500Service` 重度耦合 GORM（`createCriteria`、`Stampable`、`GrailsParameterMap`）
- `Bs500` domain 是 GORM mapping，無法直接搬

做成獨立 starter 投入產出比太低。

## Spring Boot 替代方案

| 原功能 | 替代方式 |
|--------|---------|
| 寄信 | `spring-boot-starter-mail` + `JavaMailSender.send(MimeMessage)` |
| HTML 模板 | Thymeleaf (`spring-boot-starter-thymeleaf`) 或直接組 HTML 字串 |
| 寄件記錄 (Bs500) | JPA Entity + Repository，放在業務模組內 |
| 非同步寄信 | `@Async` + `TaskExecutor`（取代原本的 `new Thread().start`）|
