package com.company.showcase.controller;

import com.company.common.log.annotation.Loggable;
import com.company.showcase.entity.Order;
import com.company.showcase.entity.Product;
import com.company.showcase.repository.OrderRepository;
import com.company.showcase.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * JPA Starter 展示用 API
 *
 * 展示 AuditableEntity（審計欄位自動填入）與 BaseEntity + SoftDeleteRepository（軟刪除）
 */
@RestController
@RequestMapping("/api/jpa-demo")
public class JpaDemoController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public JpaDemoController(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    // ==================== Product（AuditableEntity 展示）====================

    /**
     * 建立商品 — 觀察 createdBy / createdDate 自動填入
     */
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Map<String, Object> body) {
        Product product = new Product();
        product.setName((String) body.get("name"));
        product.setPrice(new BigDecimal(body.get("price").toString()));
        return ResponseEntity.ok(productRepository.save(product));
    }

    /**
     * 查詢所有商品 — 觀察審計欄位
     */
    @Loggable(logResponseBody = true)
    @GetMapping("/products")
    public ResponseEntity<List<Product>> listProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    /**
     * 更新商品 — 觀察 lastModifiedBy / lastModifiedDate 自動更新
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        if (body.containsKey("name")) {
            product.setName((String) body.get("name"));
        }
        if (body.containsKey("price")) {
            product.setPrice(new BigDecimal(body.get("price").toString()));
        }
        return ResponseEntity.ok(productRepository.save(product));
    }

    // ==================== Order（BaseEntity + SoftDeleteRepository 展示）====================

    /**
     * 建立訂單
     */
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> body) {
        Order order = new Order();
        order.setCustomerName((String) body.get("customerName"));
        order.setAmount(new BigDecimal(body.get("amount").toString()));
        order.setStatus("PENDING");
        return ResponseEntity.ok(orderRepository.save(order));
    }

    /**
     * 查詢所有未刪除的訂單 — findAllActive()
     */
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> listActiveOrders() {
        return ResponseEntity.ok(orderRepository.findAllActive());
    }

    /**
     * 查詢所有訂單（含已刪除）— findAll()，對比 findAllActive()
     */
    @GetMapping("/orders/all")
    public ResponseEntity<List<Order>> listAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    /**
     * 軟刪除訂單 — softDeleteById()
     */
    @DeleteMapping("/orders/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> softDeleteOrder(@PathVariable Long id) {
        int affected = orderRepository.softDeleteById(id);
        return ResponseEntity.ok(Map.of(
                "action", "softDelete",
                "id", id,
                "affected", affected
        ));
    }

    /**
     * 恢復訂單 — restoreById()
     */
    @PutMapping("/orders/{id}/restore")
    @Transactional
    public ResponseEntity<Map<String, Object>> restoreOrder(@PathVariable Long id) {
        int affected = orderRepository.restoreById(id);
        return ResponseEntity.ok(Map.of(
                "action", "restore",
                "id", id,
                "affected", affected
        ));
    }

    /**
     * 統計未刪除訂單數 — countActive()
     */
    @GetMapping("/orders/count")
    public ResponseEntity<Map<String, Object>> countActiveOrders() {
        return ResponseEntity.ok(Map.of(
                "activeCount", orderRepository.countActive(),
                "totalCount", orderRepository.count()
        ));
    }
}
