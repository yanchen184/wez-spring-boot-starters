package com.company.common.hub.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token 取得請求 DTO。
 */
@Getter
@Setter
@NoArgsConstructor
public class TokenRequest {

    /** 帳號。 */
    private String username;

    /** 密碼。 */
    private String password;

    /** 要存取的 API URI。 */
    private String uri;
}
