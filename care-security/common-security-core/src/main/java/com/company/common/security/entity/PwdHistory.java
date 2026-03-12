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
@Table(name = "PWD_HISTORY")
public class PwdHistory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long objid;

    @Column(name = "USERID", nullable = false)
    private Long userId;

    @Column(name = "PASSWORD_HASH", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USERID", insertable = false, updatable = false)
    private SaUser saUser;

    public Long getObjid() { return objid; }
    public void setObjid(Long objid) { this.objid = objid; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public SaUser getSaUser() { return saUser; }
    public void setSaUser(SaUser saUser) { this.saUser = saUser; }
}
