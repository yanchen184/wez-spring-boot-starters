package com.company.common.security.entity.base;

import com.company.common.jpa.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * care-security 基礎實體
 *
 * 繼承 common-jpa 的 BaseEntity（自動審計 + 軟刪除 + 樂觀鎖），
 * 用 @AttributeOverride 映射到既有 DB 欄位名。
 */
@MappedSuperclass
@AttributeOverride(name = "createdDate", column = @Column(name = "DATE_CREATED", updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "LAST_UPDATED"))
@AttributeOverride(name = "createdBy", column = @Column(name = "CREATED_BY", updatable = false))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "LAST_UPDATED_BY"))
public abstract class AuditableEntity extends BaseEntity {
}
