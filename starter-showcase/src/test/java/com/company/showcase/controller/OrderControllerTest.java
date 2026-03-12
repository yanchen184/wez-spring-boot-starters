package com.company.showcase.controller;

import com.company.common.response.code.OrderErrorCode;
import com.company.common.response.exception.BusinessException;
import com.company.showcase.entity.Order;
import com.company.showcase.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.company.common.response.config.ResponseAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderController 整合測試 — Phase 2 TDD
 *
 * 使用 Spring Boot 4 新功能：
 * - MockMvcTester（AssertJ 風格的 MockMvc）
 * - @MockitoBean（取代已廢棄的 @MockBean）
 */
@WebMvcTest(OrderController.class)
@Import(ResponseAutoConfiguration.class)
class OrderControllerTest {

    @Autowired
    MockMvcTester mockMvcTester;

    @MockitoBean
    OrderService orderService;

    @Nested
    @DisplayName("POST /api/orders — 建立訂單")
    class CreateOrderApi {

        @Test
        @DisplayName("正常請求 → 201 Created + ApiResponse 包裝正確的訂單資料")
        void shouldReturn201_withOrderData() {
            // Arrange
            Order order = new Order();
            order.setId(1L);
            order.setProductId(1L);
            order.setCustomerName("張三");
            order.setQuantity(2);
            order.setAmount(BigDecimal.valueOf(100000));
            order.setStatus("PENDING");
            when(orderService.createOrder(1L, "張三", 2)).thenReturn(order);

            // Act & Assert — MockMvcTester + AssertJ 風格
            assertThat(mockMvcTester.post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"productId": 1, "customerName": "張三", "quantity": 2}
                            """))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("商品不存在 → 404 + 錯誤碼 ORDER_B200")
        void shouldReturn404_whenProductNotFound() {
            when(orderService.createOrder(anyLong(), anyString(), anyInt()))
                    .thenThrow(new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND));

            assertThat(mockMvcTester.post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"productId": 999, "customerName": "張三", "quantity": 1}
                            """))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.code").isEqualTo("ORDER_B200");
        }

        @Test
        @DisplayName("庫存不足 → 400 + 錯誤碼 ORDER_B202")
        void shouldReturn400_whenInsufficientStock() {
            when(orderService.createOrder(anyLong(), anyString(), anyInt()))
                    .thenThrow(new BusinessException(OrderErrorCode.INSUFFICIENT_STOCK));

            assertThat(mockMvcTester.post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"productId": 1, "customerName": "張三", "quantity": 100}
                            """))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.code").isEqualTo("ORDER_B202");
        }
    }

    @Nested
    @DisplayName("POST /api/orders/{id}/cancel — 取消訂單")
    class CancelOrderApi {

        @Test
        @DisplayName("取消成功 → 200 + 狀態 CANCELLED")
        void shouldReturn200_withCancelledOrder() {
            Order cancelled = new Order();
            cancelled.setId(1L);
            cancelled.setStatus("CANCELLED");
            when(orderService.cancelOrder(1L)).thenReturn(cancelled);

            assertThat(mockMvcTester.post()
                    .uri("/api/orders/1/cancel"))
                    .hasStatus(HttpStatus.OK)
                    .bodyJson()
                    .extractingPath("$.data.status").isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("訂單不存在 → 404 + ORDER_B001")
        void shouldReturn404_whenOrderNotFound() {
            when(orderService.cancelOrder(999L))
                    .thenThrow(new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

            assertThat(mockMvcTester.post()
                    .uri("/api/orders/999/cancel"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.code").isEqualTo("ORDER_B001");
        }

        @Test
        @DisplayName("非 PENDING 訂單 → 400 + ORDER_B006")
        void shouldReturn400_whenOrderCannotCancel() {
            when(orderService.cancelOrder(1L))
                    .thenThrow(new BusinessException(OrderErrorCode.ORDER_CANNOT_CANCEL));

            assertThat(mockMvcTester.post()
                    .uri("/api/orders/1/cancel"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.code").isEqualTo("ORDER_B006");
        }
    }

    @Nested
    @DisplayName("參數驗證 — @Valid")
    class Validation {

        @Test
        @DisplayName("quantity = 0 → 400 + 驗證錯誤訊息")
        void shouldReturn400_whenQuantityIsZero() {
            assertThat(mockMvcTester.post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"productId": 1, "customerName": "張三", "quantity": 0}
                            """))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(false);

            // 驗證 Service 沒有被呼叫（請求在 Controller 層就被攔截）
            verify(orderService, never()).createOrder(anyLong(), anyString(), anyInt());
        }

        @Test
        @DisplayName("customerName 空白 → 400 + 驗證錯誤訊息")
        void shouldReturn400_whenCustomerNameBlank() {
            assertThat(mockMvcTester.post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"productId": 1, "customerName": "", "quantity": 2}
                            """))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(false);

            verify(orderService, never()).createOrder(anyLong(), anyString(), anyInt());
        }

        @Test
        @DisplayName("productId 為 null → 400 + 驗證錯誤訊息")
        void shouldReturn400_whenProductIdNull() {
            assertThat(mockMvcTester.post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"customerName": "張三", "quantity": 2}
                            """))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(false);

            verify(orderService, never()).createOrder(anyLong(), anyString(), anyInt());
        }
    }
}
