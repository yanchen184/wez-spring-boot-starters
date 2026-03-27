package com.company.common.hub.repository;

import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 授權樞紐資料存取介面。
 */
public interface HubUserSetRepository extends JpaRepository<HubUserSet, Long> {

    /**
     * 查詢使用者對指定 API 的啟用授權設定。
     *
     * @param hubUser 介接使用者
     * @param hubSet  API 設定
     * @return 啟用的授權設定
     */
    Optional<HubUserSet> findByHubUserAndHubSetAndEnabledTrue(HubUser hubUser, HubSet hubSet);

    /**
     * 查詢使用者所有啟用的授權設定。
     *
     * @param hubUser 介接使用者
     * @return 啟用的授權設定列表
     */
    List<HubUserSet> findByHubUserAndEnabledTrue(HubUser hubUser);
}
