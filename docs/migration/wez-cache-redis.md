# wez-cache-redis → 原生取代

> 狀態：✅ 原生取代（不做 Starter）

## 決策理由

- 原模組是 `spring-boot-starter-data-redis` 的薄包裝
- 自訂部分只有 `GsonRedisSerializer` 和 `HibernateProxyTypeAdapter`
- Pub/Sub 配置是標準 Spring Data Redis 寫法

## Spring Boot 替代方案

| 原功能 | 替代方式 |
|--------|---------|
| RedisTemplate + 序列化 | `spring-boot-starter-data-redis`，序列化改用 Jackson（Spring Boot 預設） |
| CacheManager | `@EnableCaching` + `RedisCacheManager`（auto-config 自動配置） |
| Pub/Sub | 搬 `RedisMessageListenerContainer` 配置到業務模組的 `@Configuration` |
| GsonRedisSerializer | 如仍需要，搬成一個 `@Bean`；建議直接用 `GenericJackson2JsonRedisSerializer` |
| HibernateProxyTypeAdapter | Hibernate 7.x 已改善 proxy 序列化，評估是否還需要 |

## application.yml 配置

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  cache:
    type: redis
```
