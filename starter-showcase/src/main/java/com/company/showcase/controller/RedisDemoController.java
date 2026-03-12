package com.company.showcase.controller;

import com.company.showcase.entity.Product;
import com.company.showcase.service.ProductCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 展示用 API
 *
 * 展示 Spring Data Redis 原生功能：
 * - RedisTemplate 基本操作（set/get/delete/TTL）
 * - @Cacheable 快取查詢（搭配 JPA Product，拆到 Service 層）
 * - @CacheEvict 清除快取
 */
@RestController
@RequestMapping("/api/redis-demo")
public class RedisDemoController {

    private static final Logger log = LoggerFactory.getLogger(RedisDemoController.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductCacheService productCacheService;

    public RedisDemoController(RedisTemplate<String, Object> redisTemplate,
                               ProductCacheService productCacheService) {
        this.redisTemplate = redisTemplate;
        this.productCacheService = productCacheService;
    }

    // ==================== RedisTemplate 基本操作 ====================

    /**
     * SET — 寫入 key-value（可選 TTL）
     */
    @PostMapping("/set")
    public ResponseEntity<Map<String, Object>> set(@RequestBody Map<String, Object> body) {
        String key = (String) body.get("key");
        Object value = body.get("value");
        int ttl = body.containsKey("ttl") ? ((Number) body.get("ttl")).intValue() : 0;

        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }

        log.info("Redis SET key={} ttl={}s", key, ttl > 0 ? ttl : "none");
        return ResponseEntity.ok(Map.of("action", "SET", "key", key, "ttl", ttl > 0 ? ttl + "s" : "none"));
    }

    /**
     * GET — 讀取 key
     */
    @GetMapping("/get/{key}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String key) {
        Object value = redisTemplate.opsForValue().get(key);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        log.info("Redis GET key={} exists={}", key, value != null);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);
        result.put("exists", value != null);
        result.put("ttl", ttl != null && ttl >= 0 ? ttl + "s" : "no expiry");
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE — 刪除 key
     */
    @DeleteMapping("/delete/{key}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String key) {
        Boolean deleted = redisTemplate.delete(key);
        log.info("Redis DEL key={} deleted={}", key, deleted);
        return ResponseEntity.ok(Map.of("action", "DEL", "key", key, "deleted", Boolean.TRUE.equals(deleted)));
    }

    /**
     * KEYS — 列出所有 key（展示用，生產環境不應使用）
     */
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> keys() {
        Set<String> keys = redisTemplate.keys("*");
        return ResponseEntity.ok(Map.of("count", keys != null ? keys.size() : 0, "keys", keys != null ? keys : Set.of()));
    }

    // ==================== @Cacheable 搭配 JPA ====================

    /**
     * 查詢商品（有快取）— 第一次打 DB，之後從 Redis 讀
     *
     * 觀察重點：
     * 1. 第一次呼叫：console 會有 Cache MISS log + Hibernate SQL log（打 DB）
     * 2. 第二次呼叫：沒有 Cache MISS log（從 Redis 快取讀取），回應更快
     * 3. 兩次都會有 --> / <-- API log（因為 @Cacheable 在 Service 層）
     */
    @GetMapping("/products/{id}")
    public Product getProductCached(@PathVariable Long id) {
        return productCacheService.getProductCached(id);
    }

    /**
     * 清除商品快取
     */
    @DeleteMapping("/products/{id}/cache")
    public ResponseEntity<Map<String, Object>> evictProductCache(@PathVariable Long id) {
        productCacheService.evictProduct(id);
        return ResponseEntity.ok(Map.of("action", "EVICT", "cache", "products", "key", id));
    }

    /**
     * 清除所有商品快取
     */
    @DeleteMapping("/products/cache")
    public ResponseEntity<Map<String, Object>> evictAllProductCache() {
        productCacheService.evictAll();
        return ResponseEntity.ok(Map.of("action", "EVICT_ALL", "cache", "products"));
    }
}
