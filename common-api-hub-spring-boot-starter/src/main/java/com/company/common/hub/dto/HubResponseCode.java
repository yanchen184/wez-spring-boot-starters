package com.company.common.hub.dto;

/**
 * Hub 統一回應代碼常數。
 *
 * <p>代碼規則：{HTTP Status}{序號}
 * <ul>
 *   <li>200xxx — 成功</li>
 *   <li>401xxx — 認證/授權失敗</li>
 *   <li>422xxx — Token 錯誤</li>
 *   <li>500xxx — 伺服器錯誤</li>
 * </ul>
 */
public final class HubResponseCode {

    private HubResponseCode() {
        // 常數類，禁止實例化
    }

    // ========== 成功 ==========

    /** 操作成功。 */
    public static final String SUCCESS = "200001";

    /** Token 簽發成功。 */
    public static final String TOKEN_ISSUED = "200002";

    /** Token 續期成功。 */
    public static final String TOKEN_REFRESHED = "200003";

    // ========== 認證失敗（401） ==========

    /** URI 不在管控範圍或 HubSet 停用。 */
    public static final String API_SET_DISABLED = "401001";

    /** 帳號不存在 / 密碼錯誤 / 帳號停用 / 授權不足。 */
    public static final String AUTH_FAILED = "401002";

    /** IP 不在白名單。 */
    public static final String IP_DENIED = "401003";

    // ========== Token 錯誤（422） ==========

    /** Token 無效（被竄改或解密失敗）。 */
    public static final String TOKEN_INVALID = "422001";

    /** Token 已過期。 */
    public static final String TOKEN_EXPIRED = "422002";

    // ========== 伺服器錯誤（500） ==========

    /** 內部錯誤。 */
    public static final String INTERNAL_ERROR = "500001";
}
