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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "SAUSER")
public class SaUser extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long objid;

    @Column(name = "USERNAME", nullable = false, unique = true)
    private String username;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "PASSWORD_SALT")
    private String passwordSalt;

    @Column(name = "DISPLAY_NAME")
    private String cname;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "ENABLED")
    private Boolean enabled = true;

    @Column(name = "ACCOUNT_LOCKED")
    private Boolean accountLocked = false;

    @Column(name = "ACCOUNT_EXPIRED")
    private Boolean accountExpired = false;

    @Column(name = "PASSWORD_EXPIRED")
    private Boolean passwordExpired = false;

    @Column(name = "IP", length = 2000)
    private String ip;

    @Column(name = "LASTLOGINDT")
    private LocalDateTime lastLoginTime;

    @Column(name = "BAD_CRED_TIME")
    private Integer loginFailCount = 0;

    @Column(name = "account_locked_date")
    private LocalDateTime lockTime;

    @Column(name = "PASSWORD_CHANGEDT")
    private LocalDateTime passwordChangedDate;

    @Column(name = "AUTH_SOURCE", length = 20)
    private String authSource = "LOCAL";

    @Column(name = "OTP_SECRET", length = 64)
    private String otpSecret;

    @Column(name = "OTP_ENABLED")
    private Boolean otpEnabled = false;

    @Column(name = "CITIZEN_ID", length = 10, unique = true)
    private String citizenId;

    /** @deprecated Legacy extensible field from Grails, no longer used. */
    @Column(name = "VALUE_1", length = 500)
    @Deprecated
    private String value1;

    /** @deprecated Legacy extensible field from Grails, no longer used. */
    @Column(name = "VALUE_2", length = 500)
    @Deprecated
    private String value2;

    /** @deprecated Legacy extensible field from Grails, no longer used. */
    @Column(name = "VALUE_3", length = 500)
    @Deprecated
    private String value3;

    /** @deprecated Legacy extensible field from Grails, no longer used. */
    @Column(name = "VALUE_4", length = 500)
    @Deprecated
    private String value4;

    /** @deprecated Legacy extensible field from Grails, no longer used. */
    @Column(name = "VALUE_5", length = 500)
    @Deprecated
    private String value5;

    @OneToMany(mappedBy = "saUser", fetch = FetchType.LAZY)
    private Set<SaUserOrgRole> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "saUser", fetch = FetchType.LAZY)
    private List<PwdHistory> pwdHistories = new ArrayList<>();

    // Getters and Setters
    public Long getObjid() { return objid; }
    public void setObjid(Long objid) { this.objid = objid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
    public String getCname() { return cname; }
    public void setCname(String cname) { this.cname = cname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAccountLocked() { return accountLocked; }
    public void setAccountLocked(Boolean accountLocked) { this.accountLocked = accountLocked; }
    public Boolean getAccountExpired() { return accountExpired; }
    public void setAccountExpired(Boolean accountExpired) { this.accountExpired = accountExpired; }
    public Boolean getPasswordExpired() { return passwordExpired; }
    public void setPasswordExpired(Boolean passwordExpired) { this.passwordExpired = passwordExpired; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public Integer getLoginFailCount() { return loginFailCount; }
    public void setLoginFailCount(Integer loginFailCount) { this.loginFailCount = loginFailCount; }
    public LocalDateTime getLockTime() { return lockTime; }
    public void setLockTime(LocalDateTime lockTime) { this.lockTime = lockTime; }
    public LocalDateTime getPasswordChangedDate() { return passwordChangedDate; }
    public void setPasswordChangedDate(LocalDateTime passwordChangedDate) { this.passwordChangedDate = passwordChangedDate; }
    @Deprecated public String getValue1() { return value1; }
    @Deprecated public void setValue1(String value1) { this.value1 = value1; }
    @Deprecated public String getValue2() { return value2; }
    @Deprecated public void setValue2(String value2) { this.value2 = value2; }
    @Deprecated public String getValue3() { return value3; }
    @Deprecated public void setValue3(String value3) { this.value3 = value3; }
    @Deprecated public String getValue4() { return value4; }
    @Deprecated public void setValue4(String value4) { this.value4 = value4; }
    @Deprecated public String getValue5() { return value5; }
    @Deprecated public void setValue5(String value5) { this.value5 = value5; }
    public String getAuthSource() { return authSource; }
    public void setAuthSource(String authSource) { this.authSource = authSource; }
    public String getOtpSecret() { return otpSecret; }
    public void setOtpSecret(String otpSecret) { this.otpSecret = otpSecret; }
    public Boolean getOtpEnabled() { return otpEnabled; }
    public void setOtpEnabled(Boolean otpEnabled) { this.otpEnabled = otpEnabled; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public Set<SaUserOrgRole> getUserRoles() { return userRoles; }
    public void setUserRoles(Set<SaUserOrgRole> userRoles) { this.userRoles = userRoles; }
    public List<PwdHistory> getPwdHistories() { return pwdHistories; }
    public void setPwdHistories(List<PwdHistory> pwdHistories) { this.pwdHistories = pwdHistories; }
}
