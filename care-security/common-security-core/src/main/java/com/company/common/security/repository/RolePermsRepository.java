package com.company.common.security.repository;

import com.company.common.security.entity.RolePerms;
import com.company.common.security.entity.id.RolePermsId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermsRepository extends JpaRepository<RolePerms, RolePermsId> {

    void deleteByRoleObjid(Long roleId);

    boolean existsByPermObjid(Long permId);
}
