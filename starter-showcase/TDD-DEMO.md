# TDD Demo — 電商訂單系統

## 需求（User Story）

> 身為前端工程師，我需要兩支 API 來完成訂單流程，
> 系統需自動處理商品驗證、庫存管理、金額計算，
> 並以統一格式回傳結果與錯誤。

### API 1：建立訂單 `POST /api/orders`

**Request Body：**
```json
{
  "productId": 1,
  "customerName": "張三",
  "quantity": 2
}
```

**Happy Path（HTTP 201）：**
1. 查詢商品是否存在
2. 檢查庫存是否足夠
3. 計算金額（單價 × 數量）
4. 扣減商品庫存
5. 建立訂單（狀態 = PENDING）
6. 回傳 `ApiResponse<Order>`

**Error Cases：**
| 情境 | 錯誤碼 | HTTP Status |
|------|--------|-------------|
| 商品不存在 | ORDER_B200 | 404 |
| 庫存不足 | ORDER_B202 | 400 |
| quantity ≤ 0 | 參數驗證 | 400 |
| customerName 空白 | 參數驗證 | 400 |

### API 2：取消訂單 `POST /api/orders/{id}/cancel`

**Happy Path（HTTP 200）：**
1. 查詢訂單是否存在
2. 檢查狀態是否為 PENDING
3. 狀態改為 CANCELLED
4. 恢復商品庫存
5. 回傳 `ApiResponse<Order>`

**Error Cases：**
| 情境 | 錯誤碼 | HTTP Status |
|------|--------|-------------|
| 訂單不存在 | ORDER_B001 | 404 |
| 非 PENDING 狀態 | ORDER_B006 | 400 |

---

## TDD 節奏（Red-Green-Refactor）

每個 commit 標記 `[RED]`、`[GREEN]` 或 `[REFACTOR]`，用 git log 即可看到完整的 TDD 過程。

### Phase 1：Service 層 — 業務邏輯（單元測試 + Mockito）
### Phase 2：Controller 層 — API 合約（MockMvcTester + @MockitoBean）
### Phase 3：參數驗證 — @Valid + 統一錯誤回應

---

## 技術棧
- Spring Boot 4.0.3 / Java 21
- JUnit 6 / Mockito / AssertJ
- MockMvcTester（Spring Boot 4 新功能）
- @MockitoBean（取代已廢棄的 @MockBean）
- common-response-starter（ApiResponse + GlobalExceptionHandler）
- common-jpa-starter（BaseEntity + SoftDeleteRepository）
