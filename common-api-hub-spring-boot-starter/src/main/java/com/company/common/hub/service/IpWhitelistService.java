package com.company.common.hub.service;

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP 白名單驗證服務。
 *
 * <p>支援以下格式：
 * <ul>
 *   <li>單一 IP 精確匹配：{@code 192.168.1.10}</li>
 *   <li>CIDR 子網路：{@code 192.168.1.0/24}</li>
 *   <li>範圍（最後一段）：{@code 192.168.1.10-20}</li>
 *   <li>多行混合：以換行分隔，只要匹配任一行即允許</li>
 * </ul>
 *
 * <p>白名單為 null 或空白時，視為不設限（全部允許）。
 */
public class IpWhitelistService {

    private static final Logger log = LoggerFactory.getLogger(IpWhitelistService.class);

    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    private final boolean allowLocal;

    public IpWhitelistService(boolean allowLocal) {
        this.allowLocal = allowLocal;
    }

    /**
     * 檢查客戶端 IP 是否在白名單內。
     *
     * @param clientIp  客戶端 IP
     * @param whitelist 白名單設定（多行）
     * @return true 表示允許
     */
    public boolean isAllowed(String clientIp, String whitelist) {
        if (whitelist == null || whitelist.isBlank()) {
            return true;
        }

        if (allowLocal && isLocalhost(clientIp)) {
            return true;
        }

        return whitelist.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .anyMatch(line -> matchLine(clientIp, line));
    }

    private boolean isLocalhost(String ip) {
        return LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip);
    }

    private boolean matchLine(String clientIp, String line) {
        // 精確匹配
        if (line.equals(clientIp)) {
            return true;
        }

        // CIDR 匹配（如 192.168.1.0/24）
        if (line.contains("/")) {
            return matchCidr(clientIp, line);
        }

        // 範圍匹配（如 192.168.1.10-20）
        if (line.matches(".*\\d+-\\d+$")) {
            return matchRange(clientIp, line);
        }

        return false;
    }

    /**
     * CIDR 子網路匹配。
     */
    private boolean matchCidr(String clientIp, String cidr) {
        try {
            SubnetUtils subnet = new SubnetUtils(cidr);
            subnet.setInclusiveHostCount(true);
            return subnet.getInfo().isInRange(clientIp);
        } catch (IllegalArgumentException e) {
            log.warn("無效的 CIDR 格式: {}", cidr);
            return false;
        }
    }

    /**
     * 範圍匹配（最後一段，如 192.168.1.10-20）。
     */
    private boolean matchRange(String clientIp, String rangeSpec) {
        String[] parts = rangeSpec.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        String lastPart = parts[3];
        String[] range = lastPart.split("-");
        if (range.length != 2) {
            return false;
        }

        String prefix = parts[0] + "." + parts[1] + "." + parts[2] + ".";
        if (!clientIp.startsWith(prefix)) {
            return false;
        }

        try {
            int clientLast = Integer.parseInt(clientIp.substring(prefix.length()));
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            return clientLast >= start && clientLast <= end;
        } catch (NumberFormatException e) {
            log.warn("無效的 IP 範圍格式: {}", rangeSpec);
            return false;
        }
    }
}
