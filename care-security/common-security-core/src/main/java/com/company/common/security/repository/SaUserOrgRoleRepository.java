package com.company.common.security.repository;

import com.company.common.security.entity.SaUserOrgRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaUserOrgRoleRepository extends JpaRepository<SaUserOrgRole, Long> {

    List<SaUserOrgRole> findByOrganizeObjid(Long organizeId);

    void deleteBySaUserObjidAndOrganizeIsNull(Long userId);

    List<SaUserOrgRole> findBySaUserObjidAndOrganizeIsNotNull(Long userId);

    Optional<SaUserOrgRole> findBySaUserObjidAndRoleObjidAndOrganizeObjid(
            Long userId, Long roleId, Long organizeId);

    Optional<SaUserOrgRole> findBySaUserObjidAndRoleObjidAndOrganizeIsNull(
            Long userId, Long roleId);

    List<SaUserOrgRole> findByOrganizeIsNotNull();

    @Query("SELECT uor FROM SaUserOrgRole uor " +
           "JOIN FETCH uor.saUser " +
           "JOIN FETCH uor.role " +
           "JOIN FETCH uor.organize " +
           "WHERE uor.organize IS NOT NULL")
    List<SaUserOrgRole> findAllWithRelations();

    @Query("SELECT uor FROM SaUserOrgRole uor " +
           "JOIN FETCH uor.role " +
           "JOIN FETCH uor.organize " +
           "WHERE uor.saUser.objid = :userId AND uor.organize IS NOT NULL")
    List<SaUserOrgRole> findByUserIdWithRelations(@Param("userId") Long userId);
}
