package com.company.showcase.service;

import com.company.showcase.entity.Order;
import com.company.showcase.entity.Product;
import com.company.showcase.repository.OrderRepository;
import com.company.showcase.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.common.response.exception.BusinessException;
import com.company.showcase.constant.OrderStatus;

/**
 * OrderService 單元測試 — TDD Demo
 *
 * 需求：電商訂單系統
 * - POST /api/orders：建立訂單（查商品→檢庫存→算金額→扣庫存→建單）
 * - POST /api/orders/{id}/cancel：取消訂單（檢狀態→改 CANCELLED→恢復庫存）
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    @Nested
    @DisplayName("建立訂單")
    class CreateOrder {

        @Test
        @DisplayName("商品存在且庫存足夠 → 建立 PENDING 訂單，金額 = 單價 × 數量")
        void shouldCreateOrder_withCorrectAmountAndStatus() {
            // Arrange
            Product product = new Product();
            product.setId(1L);
            product.setName("MacBook Pro");
            product.setPrice(BigDecimal.valueOf(50000));
            product.setStock(10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Order order = orderService.createOrder(1L, "張三", 2);

            // Assert — JUnit 6 assertAll：全部驗完再一次報告失敗
            assertAll("訂單內容",
                    () -> assertThat(order.getProductId()).isEqualTo(1L),
                    () -> assertThat(order.getCustomerName()).isEqualTo("張三"),
                    () -> assertThat(order.getQuantity()).isEqualTo(2),
                    () -> assertThat(order.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000)),
                    () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING)
            );
        }

        @Test
        @DisplayName("商品不存在 → 拋出 BusinessException，錯誤碼 ORDER_B200")
        void shouldThrow_whenProductNotFound() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(999L, "張三", 1))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getCode()).isEqualTo("ORDER_B200");
                        assertThat(bex.getHttpStatus()).isEqualTo(404);
                    });
        }

        @Test
        @DisplayName("庫存不足（庫存 1，訂購 5）→ 拋出 BusinessException，錯誤碼 ORDER_B202")
        void shouldThrow_whenInsufficientStock() {
            Product product = new Product();
            product.setId(1L);
            product.setPrice(BigDecimal.valueOf(100));
            product.setStock(1);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> orderService.createOrder(1L, "張三", 5))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getCode()).isEqualTo("ORDER_B202");
                    });
        }

        @Test
        @DisplayName("建立訂單後應扣減商品庫存（10 - 3 = 7）")
        void shouldDeductStock_whenOrderCreated() {
            Product product = new Product();
            product.setId(1L);
            product.setPrice(BigDecimal.valueOf(100));
            product.setStock(10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            orderService.createOrder(1L, "張三", 3);

            assertThat(product.getStock()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("取消訂單")
    class CancelOrder {

        @Test
        @DisplayName("PENDING 訂單取消 → 狀態改 CANCELLED，庫存恢復（7 + 3 = 10）")
        void shouldCancelOrder_andRestoreStock() {
            // Arrange
            Order order = new Order();
            order.setId(1L);
            order.setProductId(10L);
            order.setQuantity(3);
            order.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Product product = new Product();
            product.setId(10L);
            product.setStock(7);
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Order cancelled = orderService.cancelOrder(1L);

            // Assert
            assertAll("取消結果",
                    () -> assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                    () -> assertThat(product.getStock()).isEqualTo(10),
                    () -> verify(productRepository).save(product)
            );
        }

        @Test
        @DisplayName("訂單不存在 → 拋出 ORDER_B001")
        void shouldThrow_whenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getCode()).isEqualTo("ORDER_B001");
                        assertThat(bex.getHttpStatus()).isEqualTo(404);
                    });
        }

        @Test
        @DisplayName("已出貨訂單無法取消 → 拋出 ORDER_B006")
        void shouldThrow_whenOrderAlreadyShipped() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getCode()).isEqualTo("ORDER_B006");
                    });
        }
    }
}
