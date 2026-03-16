package com.company.common.security.repository;

import com.company.common.security.entity.SaUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaUserRepository extends JpaRepository<SaUser, Long> {

    Optional<SaUser> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<SaUser> findByCitizenId(String citizenId);

    @Query("SELECT u FROM SaUser u WHERE u.cname = :cname AND u.last4Idno = :last4Idno")
    Optional<SaUser> findByCnameAndLast4Idno(@Param("cname") String cname, @Param("last4Idno") String last4Idno);

    @Query("SELECT u FROM SaUser u " +
           "LEFT JOIN FETCH u.userRoles ur " +
           "LEFT JOIN FETCH ur.role " +
           "LEFT JOIN FETCH ur.organize " +
           "WHERE u.username = :username")
    Optional<SaUser> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM SaUser u " +
           "LEFT JOIN FETCH u.userRoles ur " +
           "LEFT JOIN FETCH ur.role " +
           "LEFT JOIN FETCH ur.organize")
    List<SaUser> findAllWithRoles();

    /**
     * 分頁搜尋使用者（keyword 模糊比對 username、cname、email）
     * 使用 countQuery 避免 FETCH JOIN 導致的 count 問題
     */
    @Query(value = "SELECT DISTINCT u FROM SaUser u " +
           "LEFT JOIN FETCH u.userRoles ur " +
           "LEFT JOIN FETCH ur.role " +
           "LEFT JOIN FETCH ur.organize " +
           "WHERE (:keyword IS NULL OR :keyword = '' " +
           "   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(u.cname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(DISTINCT u) FROM SaUser u " +
           "WHERE (:keyword IS NULL OR :keyword = '' " +
           "   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(u.cname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SaUser> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 分頁搜尋指定機構的使用者
     */
    @Query(value = "SELECT DISTINCT u FROM SaUser u " +
           "LEFT JOIN FETCH u.userRoles ur " +
           "LEFT JOIN FETCH ur.role " +
           "LEFT JOIN FETCH ur.organize " +
           "JOIN u.userRoles uor " +
           "WHERE uor.organize.objid = :orgId " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(u.cname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(DISTINCT u) FROM SaUser u " +
           "JOIN u.userRoles uor " +
           "WHERE uor.organize.objid = :orgId " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(u.cname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SaUser> searchUsersByOrg(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);
}
