package com.company.common.security.entity;

import com.company.common.security.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "SAUSER_ORG_ROLE")
public class SaUserOrgRole extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long objid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SAUSER_ID", nullable = false)
    private SaUser saUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORGANIZE_ID")
    private Organize organize;

    @Column(name = "CMT")
    private String comment;

    public Long getObjid() { return objid; }
    public void setObjid(Long objid) { this.objid = objid; }
    public SaUser getSaUser() { return saUser; }
    public void setSaUser(SaUser saUser) { this.saUser = saUser; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Organize getOrganize() { return organize; }
    public void setOrganize(Organize organize) { this.organize = organize; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
