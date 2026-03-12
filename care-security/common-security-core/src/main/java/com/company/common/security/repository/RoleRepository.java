package com.company.common.security.repository;

import com.company.common.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByAuthority(String authority);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.rolePerms rp LEFT JOIN FETCH rp.perm WHERE r.objid = :id")
    Optional<Role> findByIdWithPerms(@Param("id") Long id);
}
