package com.company.showcase.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 建立訂單請求 DTO
 */
@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "商品 ID 不可為空")
    private Long productId;

    @NotBlank(message = "客戶名稱不可為空")
    private String customerName;

    @NotNull(message = "數量不可為空")
    @Min(value = 1, message = "數量必須大於 0")
    private Integer quantity;
}
