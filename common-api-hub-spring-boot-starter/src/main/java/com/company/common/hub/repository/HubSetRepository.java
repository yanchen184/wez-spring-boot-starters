package com.company.common.hub.repository;

import com.company.common.hub.entity.HubSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * API 設定資料存取介面。
 */
public interface HubSetRepository extends JpaRepository<HubSet, Long> {

    /**
     * 依 URI 查詢 API 設定。
     *
     * @param uri API URI pattern
     * @return 符合的 API 設定
     */
    Optional<HubSet> findByUri(String uri);

    /**
     * 查詢所有啟用的 API 設定。
     *
     * @return 啟用的 API 設定列表
     */
    List<HubSet> findByEnabledTrue();
}
