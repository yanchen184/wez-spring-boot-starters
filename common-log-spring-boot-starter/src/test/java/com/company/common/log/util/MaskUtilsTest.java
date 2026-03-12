package com.company.common.log.util;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MaskUtils 遮罩與序列化邏輯驗證
 *
 * 直接測試 MaskUtils 的靜態方法，不需要 Spring Context。
 */
@DisplayName("MaskUtils 遮罩與序列化機制")
class MaskUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== 測試 =====

    @Nested
    @DisplayName("敏感欄位遮罩 — password/secret/token 等欄位值應變為 ***")
    class SensitiveFieldMasking {

        @Test
        @DisplayName("Map 中的 password 欄位被遮罩為 ***")
        void password欄位被遮罩() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("username", "admin");
            body.put("password", "secret123");

            String result = MaskUtils.maskAndSerialize(body, Set.of("password"), objectMapper);

            assertThat(result).contains("password:***");
            assertThat(result).doesNotContain("secret123");
            assertThat(result).contains("username:admin");
        }

        @Test
        @DisplayName("多個敏感欄位同時被遮罩")
        void 多個敏感欄位同時被遮罩() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("username", "admin");
            body.put("password", "p@ss");
            body.put("token", "abc123");
            body.put("secret", "mysecret");

            String result = MaskUtils.maskAndSerialize(body, Set.of("password", "token", "secret"), objectMapper);

            assertThat(result)
                    .contains("password:***")
                    .contains("token:***")
                    .contains("secret:***")
                    .contains("username:admin");
        }

        @Test
        @DisplayName("不在遮罩清單中的欄位保持原值")
        void 不在遮罩清單中的欄位保持原值() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("email", "test@example.com");
            body.put("name", "Bob");

            String result = MaskUtils.maskAndSerialize(body, Set.of("password"), objectMapper);

            assertThat(result)
                    .contains("email:test@example.com")
                    .contains("name:Bob");
        }
    }

    @Nested
    @DisplayName("shouldMask 大小寫不敏感比對")
    class ShouldMaskCaseInsensitive {

        @Test
        @DisplayName("欄位名稱大小寫不同時仍應遮罩")
        void 大小寫不敏感() {
            assertThat(MaskUtils.shouldMask("Password", Set.of("password"))).isTrue();
            assertThat(MaskUtils.shouldMask("PASSWORD", Set.of("password"))).isTrue();
            assertThat(MaskUtils.shouldMask("password", Set.of("password"))).isTrue();
        }

        @Test
        @DisplayName("完全不符的欄位名稱不遮罩")
        void 不符的欄位不遮罩() {
            assertThat(MaskUtils.shouldMask("email", Set.of("password"))).isFalse();
        }
    }

    @Nested
    @DisplayName("List 序列化 — 陣列型 body 正確遮罩並序列化")
    class ListSerialization {

        @Test
        @DisplayName("List 中每個元素的敏感欄位都被遮罩")
        void List中每個元素的敏感欄位都被遮罩() {
            List<Map<String, Object>> list = new ArrayList<>();

            Map<String, Object> item1 = new LinkedHashMap<>();
            item1.put("name", "user1");
            item1.put("password", "pass1");
            list.add(item1);

            Map<String, Object> item2 = new LinkedHashMap<>();
            item2.put("name", "user2");
            item2.put("password", "pass2");
            list.add(item2);

            String result = MaskUtils.maskAndSerialize(list, Set.of("password"), objectMapper);

            assertThat(result)
                    .startsWith("[")
                    .endsWith("]")
                    .contains("name:user1")
                    .contains("name:user2")
                    .contains("password:***")
                    .doesNotContain("pass1")
                    .doesNotContain("pass2");
        }

        @Test
        @DisplayName("空 List 序列化為 []")
        void 空List序列化為空陣列() {
            String result = MaskUtils.maskAndSerialize(new ArrayList<>(), Set.of("password"), objectMapper);

            assertThat(result).isEqualTo("[]");
        }
    }

    @Nested
    @DisplayName("toCompactString — Map 轉為簡潔格式")
    class CompactStringFormat {

        @Test
        @DisplayName("Map 轉為 {key1:value1, key2:value2} 格式")
        void Map轉為簡潔格式() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("a", 1);
            map.put("b", "hello");

            String result = MaskUtils.toCompactString(map);

            assertThat(result).isEqualTo("{a:1, b:hello}");
        }

        @Test
        @DisplayName("空 Map 轉為 {}")
        void 空Map轉為空大括號() {
            String result = MaskUtils.toCompactString(new LinkedHashMap<>());

            assertThat(result).isEqualTo("{}");
        }
    }

    @Nested
    @DisplayName("非 Map 類型序列化 — 直接 toString")
    class NonMapSerialization {

        @Test
        @DisplayName("純字串直接回傳 toString")
        void 純字串直接toString() {
            String result = MaskUtils.maskAndSerialize("hello world", Set.of("password"), objectMapper);

            assertThat(result).isEqualTo("hello world");
        }
    }

    @Nested
    @DisplayName("maskQueryParams — query parameter 遮罩")
    class QueryParamMasking {

        @Test
        @DisplayName("query parameter 中的敏感欄位被遮罩")
        void query_parameter中的敏感欄位被遮罩() {
            Map<String, String[]> paramMap = new LinkedHashMap<>();
            paramMap.put("username", new String[]{"admin"});
            paramMap.put("password", new String[]{"secret123"});

            String result = MaskUtils.maskQueryParams(paramMap, Set.of("password"));

            assertThat(result).contains("password:***");
            assertThat(result).doesNotContain("secret123");
            assertThat(result).contains("username:admin");
        }

        @Test
        @DisplayName("空 paramMap 返回 null")
        void 空paramMap返回null() {
            assertThat(MaskUtils.maskQueryParams(null, Set.of("password"))).isNull();
            assertThat(MaskUtils.maskQueryParams(Map.of(), Set.of("password"))).isNull();
        }
    }
}
