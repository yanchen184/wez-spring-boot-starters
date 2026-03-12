package com.company.showcase.repository;

import com.company.showcase.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 商品 Repository — 標準 JpaRepository（不需要軟刪除）
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
