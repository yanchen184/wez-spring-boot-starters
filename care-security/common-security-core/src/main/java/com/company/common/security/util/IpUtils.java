package com.company.common.security.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * Extract client IP with basic validation to prevent log spoofing via X-Forwarded-For.
 */
public final class IpUtils {

    private static final Pattern VALID_IP = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
                    + "|^[0-9a-fA-F:]+$");

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String ip = xForwardedFor.split(",")[0].trim();
            if (VALID_IP.matcher(ip).matches()) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
}
