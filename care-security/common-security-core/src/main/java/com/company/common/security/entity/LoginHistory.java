package com.company.common.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "LOGIN_HISTORY")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long objid;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "IP", length = 45)
    private String ipAddress;

    @Column(name = "IS_SUCCESS")
    private Boolean success;

    @Column(name = "LOGIN_DATETIME")
    private LocalDateTime loginTime;

    @Column(name = "FAILURE_MESSAGE")
    private String failReason;

    @PrePersist
    protected void onCreate() {
        if (this.loginTime == null) {
            this.loginTime = LocalDateTime.now();
        }
    }

    public Long getObjid() { return objid; }
    public void setObjid(Long objid) { this.objid = objid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
}
