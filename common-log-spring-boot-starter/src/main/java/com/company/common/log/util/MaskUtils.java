package com.company.common.log.util;

import tools.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 敏感欄位遮罩與序列化工具
 *
 * 將 Map / List / POJO 序列化為簡潔格式 {key:value}，
 * 並自動遮罩 maskFields 指定的欄位值為 ***
 */
public final class MaskUtils {

    private static final String MASK = "***";

    private MaskUtils() {
        // utility class
    }

    /**
     * 將物件序列化為簡潔字串，並遮罩敏感欄位
     *
     * @param body         要序列化的物件（Map / List / POJO / String）
     * @param maskFields   要遮罩的欄位名稱集合
     * @param objectMapper Jackson ObjectMapper（用於 POJO → Map 轉換）
     * @return 序列化後的字串，例如 {username:admin, password:***}
     */
    @SuppressWarnings("unchecked")
    public static String maskAndSerialize(Object body, Set<String> maskFields, ObjectMapper objectMapper) {
        try {
            if (body instanceof List<?> list) {
                return maskAndSerializeList(list, maskFields, objectMapper);
            }
            // 先轉成 Map 以便遮罩欄位
            Map<String, Object> map;
            if (body instanceof Map) {
                map = new LinkedHashMap<>((Map<String, Object>) body);
            } else {
                map = objectMapper.convertValue(body, objectMapper.getTypeFactory()
                        .constructMapType(LinkedHashMap.class, String.class, Object.class));
            }
            maskFields.forEach(field -> {
                if (map.containsKey(field)) {
                    map.put(field, MASK);
                }
            });
            return toCompactString(map);
        } catch (Exception e) {
            // 非 Map 類型（如 String），直接 toString
            return body.toString();
        }
    }

    /**
     * 序列化 List，每個元素遞迴遮罩
     */
    private static String maskAndSerializeList(List<?> list, Set<String> maskFields, ObjectMapper objectMapper) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(maskAndSerialize(list.get(i), maskFields, objectMapper));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * 將 Map 轉為簡潔格式：{key1:value1, key2:value2}
     */
    public static String toCompactString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append(':').append(entry.getValue());
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * 判斷欄位名稱是否需要遮罩（大小寫不敏感）
     */
    public static boolean shouldMask(String fieldName, Set<String> maskFields) {
        for (String mask : maskFields) {
            if (mask.equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 遮罩 query parameters
     *
     * @param paramMap   HttpServletRequest.getParameterMap()
     * @param maskFields 要遮罩的欄位名稱集合
     * @return 序列化後的字串，例如 {username:admin, password:***}，或 null 若無參數
     */
    public static String maskQueryParams(Map<String, String[]> paramMap, Set<String> maskFields) {
        if (paramMap == null || paramMap.isEmpty()) return null;

        Map<String, Object> params = new LinkedHashMap<>();
        paramMap.forEach((key, values) -> {
            if (shouldMask(key, maskFields)) {
                params.put(key, MASK);
            } else {
                params.put(key, values.length == 1 ? values[0] : values);
            }
        });
        return toCompactString(params);
    }
}
