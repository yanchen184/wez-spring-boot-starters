package com.company.common.hub.repository;

import com.company.common.hub.entity.HubLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * 呼叫日誌資料存取介面。
 */
public interface HubLogRepository extends JpaRepository<HubLog, Long> {

    /**
     * 刪除指定時間之前的日誌（用於排程清理）。
     *
     * @param before 時間門檻
     */
    void deleteByCreatedAtBefore(LocalDateTime before);
}
