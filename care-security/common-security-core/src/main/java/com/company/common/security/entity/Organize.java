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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ORGANIZE")
public class Organize extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long objid;

    @Column(name = "DISPLAY_NAME")
    private String orgName;

    @Column(name = "CODE", length = 100)
    private String orgCode;

    @Column(name = "PARENT_ID")
    private Long parentId;

    @Column(name = "SEQ")
    private Integer sortOrder;

    @Column(name = "ENABLED")
    private Boolean enabled = true;

    @Column(name = "TEL")
    private String tel;

    @Column(name = "ADDR")
    private String addr;

    @Column(name = "TWNSPCODE")
    private String twnspCode;

    @Column(name = "CNTCODE")
    private String cntCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID", insertable = false, updatable = false)
    private Organize parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Organize> children = new ArrayList<>();

    @OneToMany(mappedBy = "organize", fetch = FetchType.LAZY)
    private List<SaUserOrgRole> userOrgRoles = new ArrayList<>();

    public Long getObjid() { return objid; }
    public void setObjid(Long objid) { this.objid = objid; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }
    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }
    public String getTwnspCode() { return twnspCode; }
    public void setTwnspCode(String twnspCode) { this.twnspCode = twnspCode; }
    public String getCntCode() { return cntCode; }
    public void setCntCode(String cntCode) { this.cntCode = cntCode; }
    public Organize getParent() { return parent; }
    public void setParent(Organize parent) { this.parent = parent; }
    public List<Organize> getChildren() { return children; }
    public void setChildren(List<Organize> children) { this.children = children; }
    public List<SaUserOrgRole> getUserOrgRoles() { return userOrgRoles; }
    public void setUserOrgRoles(List<SaUserOrgRole> userOrgRoles) { this.userOrgRoles = userOrgRoles; }
}
