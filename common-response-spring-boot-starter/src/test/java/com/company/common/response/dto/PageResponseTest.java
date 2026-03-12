package com.company.common.response.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageResponse 分頁回應驗證
 *
 * 確保分頁計算邏輯（totalPages, first, last, hasNext, hasPrevious）正確。
 */
@DisplayName("PageResponse 分頁回應")
class PageResponseTest {

    @Nested
    @DisplayName("分頁計算邏輯")
    class PaginationCalculation {

        @Test
        @DisplayName("第一頁：first=true, hasPrevious=false")
        void 第一頁() {
            PageResponse<String> page = PageResponse.of(
                    List.of("a", "b"), 0, 10, 25);

            assertThat(page.isFirst()).isTrue();
            assertThat(page.isHasPrevious()).isFalse();
            assertThat(page.getPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("最後一頁：last=true, hasNext=false")
        void 最後一頁() {
            // 25 筆，每頁 10 筆，共 3 頁（0, 1, 2）
            PageResponse<String> page = PageResponse.of(
                    List.of("x"), 2, 10, 25);

            assertThat(page.isLast()).isTrue();
            assertThat(page.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("中間頁：hasNext=true, hasPrevious=true")
        void 中間頁() {
            PageResponse<String> page = PageResponse.of(
                    List.of("a", "b"), 1, 10, 25);

            assertThat(page.isFirst()).isFalse();
            assertThat(page.isLast()).isFalse();
            assertThat(page.isHasNext()).isTrue();
            assertThat(page.isHasPrevious()).isTrue();
        }

        @Test
        @DisplayName("totalPages 計算正確（無條件進位）")
        void totalPages計算正確() {
            // 25 / 10 = 2.5 -> 3 頁
            PageResponse<String> page = PageResponse.of(List.of(), 0, 10, 25);
            assertThat(page.getTotalPages()).isEqualTo(3);

            // 20 / 10 = 2.0 -> 2 頁
            PageResponse<String> page2 = PageResponse.of(List.of(), 0, 10, 20);
            assertThat(page2.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("size=0 時 totalPages=0，不除零錯誤")
        void size為零時不除零錯誤() {
            PageResponse<String> page = PageResponse.of(List.of(), 0, 0, 10);

            assertThat(page.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("totalElements=0 時，first=true, last=true")
        void 無資料時() {
            PageResponse<String> page = PageResponse.of(List.of(), 0, 10, 0);

            assertThat(page.getTotalPages()).isEqualTo(0);
            assertThat(page.isFirst()).isTrue();
            // page=0, totalPages=0 -> last = (0 >= 0-1) = true
            assertThat(page.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("toApiResponse 包裝")
    class ToApiResponse {

        @Test
        @DisplayName("toApiResponse 回傳 success=true 的 ApiResponse")
        void 包裝為ApiResponse() {
            PageResponse<String> page = PageResponse.of(
                    List.of("a", "b"), 0, 10, 2);

            ApiResponse<PageResponse<String>> response = page.toApiResponse();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(page);
            assertThat(response.getData().getContent()).hasSize(2);
        }
    }
}
