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
@Table(name = "MENU")
public class Menu extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long objid;

    @Column(name = "DISPLAY_NAME")
    private String menuName;

    @Column(name = "CODE", length = 100)
    private String menuCode;

    @Column(name = "URL", length = 500)
    private String url;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "PARENT_ID")
    private Long parentId;

    @Column(name = "SEQ")
    private Integer sortOrder;

    @Column(name = "ICON")
    private String icon;

    @Column(name = "ENABLED")
    private Boolean enabled = true;

    @Column(name = "AUTH_NAME")
    private String authName;

    @Column(name = "CONTEXT_PATH")
    private String contextPath;

    @Column(name = "ISP")
    private Boolean isp;

    @Column(name = "C_PERM_ALIAS", length = 1000)
    private String cPermAlias;

    @Column(name = "R_PERM_ALIAS", length = 1000)
    private String rPermAlias;

    @Column(name = "U_PERM_ALIAS", length = 1000)
    private String uPermAlias;

    @Column(name = "D_PERM_ALIAS", length = 1000)
    private String dPermAlias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID", insertable = false, updatable = false)
    private Menu parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Menu> children = new ArrayList<>();

    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY)
    private List<Perm> perms = new ArrayList<>();

    public Long getObjid() { return objid; }
    public void setObjid(Long objid) { this.objid = objid; }
    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }
    public String getMenuCode() { return menuCode; }
    public void setMenuCode(String menuCode) { this.menuCode = menuCode; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getAuthName() { return authName; }
    public void setAuthName(String authName) { this.authName = authName; }
    public String getContextPath() { return contextPath; }
    public void setContextPath(String contextPath) { this.contextPath = contextPath; }
    public Boolean getIsp() { return isp; }
    public void setIsp(Boolean isp) { this.isp = isp; }
    public String getCPermAlias() { return cPermAlias; }
    public void setCPermAlias(String cPermAlias) { this.cPermAlias = cPermAlias; }
    public String getRPermAlias() { return rPermAlias; }
    public void setRPermAlias(String rPermAlias) { this.rPermAlias = rPermAlias; }
    public String getUPermAlias() { return uPermAlias; }
    public void setUPermAlias(String uPermAlias) { this.uPermAlias = uPermAlias; }
    public String getDPermAlias() { return dPermAlias; }
    public void setDPermAlias(String dPermAlias) { this.dPermAlias = dPermAlias; }
    public Menu getParent() { return parent; }
    public void setParent(Menu parent) { this.parent = parent; }
    public List<Menu> getChildren() { return children; }
    public void setChildren(List<Menu> children) { this.children = children; }
    public List<Perm> getPerms() { return perms; }
    public void setPerms(List<Perm> perms) { this.perms = perms; }
}
