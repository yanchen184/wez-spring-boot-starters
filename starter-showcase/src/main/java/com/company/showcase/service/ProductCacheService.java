package com.company.showcase.service;

import com.company.showcase.entity.Product;
import com.company.showcase.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 商品快取 Service — 把 @Cacheable 從 controller 拆到 service 層
 *
 * 這樣 controller 層的 API log（--> / <--）不會被快取跳過
 */
@Service
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);

    private final ProductRepository productRepository;

    public ProductCacheService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "products", key = "#id")
    public Product getProductCached(Long id) {
        log.info("Cache MISS — querying DB for product id={}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @CacheEvict(value = "products", key = "#id")
    public void evictProduct(Long id) {
        log.info("Cache EVICT — product id={}", id);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void evictAll() {
        log.info("Cache EVICT ALL — products");
    }
}
