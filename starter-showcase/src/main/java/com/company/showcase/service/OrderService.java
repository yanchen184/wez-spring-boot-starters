package com.company.showcase.service;

import com.company.common.response.code.OrderErrorCode;
import com.company.common.response.exception.BusinessException;
import com.company.showcase.constant.OrderStatus;
import com.company.showcase.entity.Order;
import com.company.showcase.entity.Product;
import com.company.showcase.repository.OrderRepository;
import com.company.showcase.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 訂單服務 — TDD 驅動設計
 */
@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public Order createOrder(Long productId, String customerName, int quantity) {
        Product product = validateAndGetProduct(productId, quantity);
        deductStock(product, quantity);
        return saveOrder(product, customerName, quantity);
    }

    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order.getProductId(), order.getQuantity());
        return orderRepository.save(order);
    }

    private Product validateAndGetProduct(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStock() < quantity) {
            throw new BusinessException(OrderErrorCode.INSUFFICIENT_STOCK);
        }
        return product;
    }

    private void deductStock(Product product, int quantity) {
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    private void restoreStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND));
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }

    private Order saveOrder(Product product, String customerName, int quantity) {
        Order order = new Order();
        order.setProductId(product.getId());
        order.setCustomerName(customerName);
        order.setQuantity(quantity);
        order.setAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(OrderStatus.PENDING);
        return orderRepository.save(order);
    }
}
