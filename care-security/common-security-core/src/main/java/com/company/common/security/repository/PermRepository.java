package com.company.common.security.repository;

import com.company.common.security.entity.Perm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermRepository extends JpaRepository<Perm, Long> {

    @Query("SELECT p FROM Perm p LEFT JOIN FETCH p.menu")
    List<Perm> findAllWithMenu();

    @Query("SELECT p FROM Perm p LEFT JOIN FETCH p.menu JOIN RolePerms rp ON rp.perm = p JOIN rp.role r WHERE r.objid = :roleId")
    List<Perm> findByRoleIdWithMenu(@Param("roleId") Long roleId);

    @Query("SELECT DISTINCT p FROM Perm p " +
           "LEFT JOIN FETCH p.menu " +
           "JOIN RolePerms rp ON rp.perm = p " +
           "JOIN rp.role r " +
           "JOIN SaUserOrgRole uor ON uor.role = r " +
           "WHERE uor.saUser.objid = :userId " +
           "AND (uor.organize IS NULL OR uor.organize.objid = :orgId)")
    List<Perm> findByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("SELECT DISTINCT p FROM Perm p " +
           "LEFT JOIN FETCH p.menu " +
           "JOIN RolePerms rp ON rp.perm = p " +
           "JOIN rp.role r " +
           "JOIN SaUserOrgRole uor ON uor.role = r " +
           "WHERE uor.saUser.objid = :userId")
    List<Perm> findByUserIdAllRoles(@Param("userId") Long userId);
}
