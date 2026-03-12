package com.company.common.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 審計基礎實體（推薦大部分實體繼承此類）
 *
 * <p>自動記錄 4 個審計欄位：
 * <ul>
 *   <li>{@code createdDate} — 建立時間（自動填入，不可更新）</li>
 *   <li>{@code lastModifiedDate} — 最後修改時間（每次更新自動填入）</li>
 *   <li>{@code createdBy} — 建立人（從 SecurityContext 自動取得）</li>
 *   <li>{@code lastModifiedBy} — 最後修改人（從 SecurityContext 自動取得）</li>
 * </ul>
 *
 * <p>不包含 {@code @Id}，由子類別自行定義主鍵策略。
 *
 * <p>如果需要軟刪除和樂觀鎖，請改用 {@link BaseEntity}。
 *
 * @see BaseEntity
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;
}
