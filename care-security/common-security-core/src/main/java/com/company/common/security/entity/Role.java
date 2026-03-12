package com.company.common.security.entity;

import com.company.common.security.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ROLE")
public class Role extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long objid;

    @Column(name = "AUTHORITY", nullable = false, unique = true)
    private String authority;

    @Column(name = "DISPLAY_NAME", length = 500)
    private String description;

    @Column(name = "IS_VISIBLE")
    private Boolean enabled = true;

    @Column(name = "IS_EDITABLE")
    private Boolean editable = true;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<RolePerms> rolePerms = new ArrayList<>();

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<SaUserOrgRole> userRoles = new ArrayList<>();

    public Long getObjid() { return objid; }
    public void setObjid(Long objid) { this.objid = objid; }
    public String getAuthority() { return authority; }
    public void setAuthority(String authority) { this.authority = authority; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getEditable() { return editable; }
    public void setEditable(Boolean editable) { this.editable = editable; }
    public List<RolePerms> getRolePerms() { return rolePerms; }
    public void setRolePerms(List<RolePerms> rolePerms) { this.rolePerms = rolePerms; }
    public List<SaUserOrgRole> getUserRoles() { return userRoles; }
    public void setUserRoles(List<SaUserOrgRole> userRoles) { this.userRoles = userRoles; }
}
