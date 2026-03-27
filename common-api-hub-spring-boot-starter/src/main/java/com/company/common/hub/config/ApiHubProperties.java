package com.company.common.hub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * API Hub 配置屬性。
 *
 * <pre>
 * common:
 *   api-hub:
 *     enabled: true
 *     jwt:
 *       secret-key: your-secret-key-at-least-32-chars
 *     ip-whitelist:
 *       allow-local: true
 *     log:
 *       mask-fields:
 *         - password
 *         - passcode
 *         - secret
 *       retention-days: 90
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "common.api-hub")
public class ApiHubProperties {

    /** 是否啟用 API Hub。 */
    private boolean enabled = false;

    /** JWT 相關設定。 */
    private Jwt jwt = new Jwt();

    /** IP 白名單相關設定。 */
    private IpWhitelist ipWhitelist = new IpWhitelist();

    /** 日誌相關設定。 */
    private Log log = new Log();

    @Getter
    @Setter
    public static class Jwt {
        /** JWT 密鑰（至少 32 字元）。 */
        private String secretKey = "default-api-hub-secret-key-change-me!!";
    }

    @Getter
    @Setter
    public static class IpWhitelist {
        /** 是否允許 localhost。 */
        private boolean allowLocal = true;
    }

    @Getter
    @Setter
    public static class Log {
        /** 需脫敏的欄位名稱列表。 */
        private List<String> maskFields = List.of("password", "passcode", "secret");

        /** 日誌保留天數。 */
        private int retentionDays = 90;
    }
}
