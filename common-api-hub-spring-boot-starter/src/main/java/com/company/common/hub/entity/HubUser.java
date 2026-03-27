package com.company.common.hub.entity;

import com.company.common.jpa.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 介接使用者實體。
 *
 * <p>代表一個外部系統的認證帳號，包含帳號、密碼（BCrypt）、IP 白名單設定。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "HUB_USER")
public class HubUser extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long id;

    /** 帳號（唯一）。 */
    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
    private String username;

    /** BCrypt 加密密碼。 */
    @Column(name = "PASSWORD", nullable = false)
    private String password;

    /** 單位 ID（nullable）。 */
    @Column(name = "ORG_ID")
    private Long orgId;

    /** IP 白名單（多行，支援 CIDR / 範圍）。 */
    @Column(name = "VERIFY_IP", length = 2000)
    private String verifyIp;

    /** 啟用狀態。 */
    @Column(name = "ENABLED")
    private Boolean enabled = true;
}
