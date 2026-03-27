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
 * API 設定實體。
 *
 * <p>定義一組受管控的 API，包含 URI pattern、JWT Token 有效秒數及啟用狀態。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "HUB_SET")
public class HubSet extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long id;

    /** API 名稱。 */
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    /** API URI pattern（支援 Ant 風格，如 /api/users/**）。 */
    @Column(name = "URI", nullable = false, length = 200)
    private String uri;

    /** JWT Token 有效秒數，預設 3600。 */
    @Column(name = "JWT_TOKEN_AGING")
    private Integer jwtTokenAging = 3600;

    /** 啟用狀態。 */
    @Column(name = "ENABLED")
    private Boolean enabled = true;

    /** 備註說明。 */
    @Column(name = "DESCRIPTION", length = 1000)
    private String description;
}
