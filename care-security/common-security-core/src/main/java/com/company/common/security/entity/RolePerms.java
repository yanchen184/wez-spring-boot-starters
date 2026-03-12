package com.company.common.security.entity;

import com.company.common.security.entity.id.RolePermsId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ROLE_PERMS")
@IdClass(RolePermsId.class)
public class RolePerms {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID")
    private Role role;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERM_ID")
    private Perm perm;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Perm getPerm() { return perm; }
    public void setPerm(Perm perm) { this.perm = perm; }
}
