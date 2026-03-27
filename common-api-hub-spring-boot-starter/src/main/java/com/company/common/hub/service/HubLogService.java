package com.company.common.hub.service;

import com.company.common.hub.entity.HubLog;
import com.company.common.hub.entity.HubUserSet;
import com.company.common.hub.repository.HubLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 日誌記錄服務。
 *
 * <p>提供敏感欄位脫敏（password / passcode / secret / token）
 * 及日誌建立功能。
 */
public class HubLogService {

    private static final Logger log = LoggerFactory.getLogger(HubLogService.class);

    private final HubLogRepository hubLogRepository;
    private final Pattern maskPattern;

    /**
     * 建構子。
     *
     * @param hubLogRepository 日誌 Repository
     * @param sensitiveFields  需脫敏的欄位名稱列表
     */
    public HubLogService(HubLogRepository hubLogRepository, List<String> sensitiveFields) {
        this.hubLogRepository = hubLogRepository;
        this.maskPattern = buildMaskPattern(sensitiveFields);
    }

    /**
     * 對 JSON 字串中的敏感欄位值替換為 "***"。
     *
     * @param json JSON 字串（nullable）
     * @return 脫敏後的字串，null 輸入回傳 null
     */
    public String maskSensitiveFields(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        return maskPattern.matcher(json).replaceAll("$1\"***\"");
    }

    /**
     * 建立呼叫日誌並存入 DB。
     *
     * @param hubUserSet     授權設定（nullable）
     * @param requestMethod  HTTP 方法
     * @param requestUri     請求 URI
     * @param requestParams  請求參數（會自動脫敏）
     * @param ip             客戶端 IP
     * @param success        是否成功
     * @param responseCode   回傳代碼
     * @param responseResult 回傳內容
     * @param errorLog       錯誤訊息
     * @param elapsedMs      耗時（毫秒）
     * @return 已儲存的 HubLog
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public HubLog log(HubUserSet hubUserSet,
                      String requestMethod,
                      String requestUri,
                      String requestParams,
                      String ip,
                      Boolean success,
                      String responseCode,
                      String responseResult,
                      String errorLog,
                      Long elapsedMs) {
        HubLog hubLog = new HubLog();
        hubLog.setHubUserSet(hubUserSet);
        hubLog.setRequestMethod(requestMethod);
        hubLog.setRequestUri(requestUri);
        hubLog.setRequestParams(maskSensitiveFields(requestParams));
        hubLog.setIp(ip);
        hubLog.setSuccess(success);
        hubLog.setResponseCode(responseCode);
        hubLog.setResponseResult(responseResult);
        hubLog.setErrorLog(errorLog);
        hubLog.setElapsedMs(elapsedMs);

        return hubLogRepository.save(hubLog);
    }

    /**
     * 建構敏感欄位的正則 Pattern。
     *
     * <p>匹配 JSON 格式如 "password":"value" 或 "password" : "value"
     */
    private Pattern buildMaskPattern(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return Pattern.compile("(?!)");
        }
        String joined = String.join("|", fields);
        String regex = "(\"(?:" + joined + ")\"\\s*:\\s*)\"[^\"]*\"";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }
}
