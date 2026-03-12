package com.company.common.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * 完整基礎實體 - 審計 + 軟刪除 + 樂觀鎖
 *
 * <p>繼承 {@link AuditableEntity} 的 4 個審計欄位，
 * 額外提供 {@code deleted}（軟刪除）和 {@code version}（樂觀鎖）。
 *
 * <p><strong>大部分實體不需要這個。</strong>
 * 除非業務明確需要軟刪除或樂觀鎖，否則直接繼承 {@link AuditableEntity}。
 *
 * <p>搭配 {@link com.company.common.jpa.repository.SoftDeleteRepository} 使用。
 *
 * <p>不包含 {@code @Id}，由子類別自行定義主鍵策略。
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity extends AuditableEntity {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Version
    @Column(name = "version")
    private Integer version;

    public void delete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }
}
