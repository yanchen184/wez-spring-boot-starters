package com.company.common.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記需要自動記錄日誌的方法或類別
 *
 * 使用方式：
 * 1. 標記在 Controller 類別上 - 該 Controller 所有方法都會記錄
 * 2. 標記在方法上 - 只有該方法會記錄（方法層級優先於類別層級）
 *
 * 範例：
 * <pre>
 * {@code
 * @Loggable
 * @RestController
 * public class OrderController {
 *     // 所有方法自動記錄 log
 * }
 *
 * // 或
 * @RestController
 * public class UserController {
 *
 *     @Loggable(logResponseBody = true)
 *     @GetMapping("/users")
 *     public List<User> getUsers() { ... }
 *
 *     @Loggable(logRequestBody = false)  // --> log 不帶 body
 *     @PostMapping("/sensitive")
 *     public String sensitive(@RequestBody Map body) { ... }
 * }
 * }
 * </pre>
 *
 * @author Platform Team
 * @version 2.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {

    /**
     * 是否在 --> log 中記錄請求 Body
     * 設定為 false 時，--> log 不含 body={...}
     * 預設：true
     */
    boolean logRequestBody() default true;

    /**
     * 是否記錄回應 Body（遮罩敏感欄位後印出）
     * 設定為 true 時，<-- log 包含 body={...}
     * 預設：false
     */
    boolean logResponseBody() default false;

    /**
     * 需要遮罩的欄位名稱（敏感資料保護）
     * 例如：{"password", "creditCard", "idNumber"}
     */
    String[] maskFields() default {"password", "pwd", "secret", "token", "authorization"};

    /**
     * 慢請求閾值（毫秒）
     * -1 表示使用全域配置（common.log.slow-threshold-ms）
     * 設定正數時優先於全域配置
     */
    long slowThresholdMs() default -1;

}
