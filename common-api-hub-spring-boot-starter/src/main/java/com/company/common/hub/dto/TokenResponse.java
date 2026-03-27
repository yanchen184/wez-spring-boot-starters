package com.company.common.hub.dto;

import lombok.Getter;

/**
 * Token 回應 DTO（不可變物件）。
 */
@Getter
public class TokenResponse {

    private final String token;
    private final int expiresIn;

    public TokenResponse(String token, int expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}
