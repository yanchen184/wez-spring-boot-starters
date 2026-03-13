package com.company.common.security.repository;

import com.company.common.security.entity.SaUser;
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
}
