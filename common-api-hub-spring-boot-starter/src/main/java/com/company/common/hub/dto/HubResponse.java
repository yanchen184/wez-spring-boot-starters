package com.company.common.hub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * Hub 統一回應格式。
 *
 * @param <T> data 的型別
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HubResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    private HubResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功回應（含 data）。
     */
    public static <T> HubResponse<T> success(String code, String message, T data) {
        return new HubResponse<>(code, message, data);
    }

    /**
     * 成功回應（無 data）。
     */
    public static HubResponse<Void> success(String code, String message) {
        return new HubResponse<>(code, message, null);
    }

    /**
     * 失敗回應。
     */
    public static HubResponse<Void> error(String code, String message) {
        return new HubResponse<>(code, message, null);
    }
}
