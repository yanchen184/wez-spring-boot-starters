package com.company.common.hub.entity;

import com.company.common.jpa.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 授權樞紐實體。
 *
 * <p>定義某個介接使用者（HubUser）對某個 API 設定（HubSet）的存取權限，
 * 包含有效期間、驗證模式（帳密 / Token）及啟用狀態。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "HUB_USER_SET")
public class HubUserSet extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long id;

    /** 關聯 API 設定。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HUB_SET_ID", nullable = false)
    private HubSet hubSet;

    /** 關聯介接使用者。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HUB_USER_ID", nullable = false)
    private HubUser hubUser;

    /** 有效日期起。 */
    @Column(name = "VERIFY_DTS")
    private LocalDate verifyDts;

    /** 有效日期迄。 */
    @Column(name = "VERIFY_DTE")
    private LocalDate verifyDte;

    /** 啟用帳密驗證。 */
    @Column(name = "USER_VERIFY")
    private Boolean userVerify = true;

    /** 啟用 Token 驗證。 */
    @Column(name = "JWT_TOKEN_VERIFY")
    private Boolean jwtTokenVerify = true;

    /** 啟用狀態。 */
    @Column(name = "ENABLED")
    private Boolean enabled = true;
}
