package com.company.common.security.entity.id;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class RolePermsId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long role;
    private Long perm;

    public RolePermsId() {}

    public RolePermsId(Long role, Long perm) {
        this.role = role;
        this.perm = perm;
    }

    public Long getRole() { return role; }
    public void setRole(Long role) { this.role = role; }
    public Long getPerm() { return perm; }
    public void setPerm(Long perm) { this.perm = perm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePermsId that = (RolePermsId) o;
        return Objects.equals(role, that.role) && Objects.equals(perm, that.perm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, perm);
    }
}
