package com.company.common.hub.repository;

import com.company.common.hub.entity.HubUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 介接使用者資料存取介面。
 */
public interface HubUserRepository extends JpaRepository<HubUser, Long> {

    /**
     * 依帳號查詢使用者。
     *
     * @param username 帳號
     * @return 符合的使用者
     */
    Optional<HubUser> findByUsername(String username);

    /**
     * 依帳號查詢啟用的使用者。
     *
     * @param username 帳號
     * @return 啟用且符合的使用者
     */
    Optional<HubUser> findByUsernameAndEnabledTrue(String username);
}
