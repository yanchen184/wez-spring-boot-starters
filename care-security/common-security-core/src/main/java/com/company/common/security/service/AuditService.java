package com.company.common.security.service;

import com.company.common.security.entity.AuditLog;
import com.company.common.security.entity.LoginHistory;
import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.AuditLogRepository;
import com.company.common.security.repository.LoginHistoryRepository;
import org.springframework.transaction.annotation.Transactional;

public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    public AuditService(AuditLogRepository auditLogRepository,
                        LoginHistoryRepository loginHistoryRepository) {
        this.auditLogRepository = auditLogRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Transactional
    public void logEvent(String eventType, String username, String targetUser,
                         String ipAddress, String userAgent, String detail) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setUsername(username);
        log.setTargetUser(targetUser);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    @Transactional
    public void logLogin(SaUser user, String ipAddress, String userAgent,
                         boolean success, String failReason) {
        LoginHistory history = new LoginHistory();
        history.setUsername(user.getUsername());
        history.setIpAddress(ipAddress);
        history.setSuccess(success);
        history.setFailReason(failReason);
        loginHistoryRepository.save(history);
    }
}
