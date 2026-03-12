package com.company.common.jpa.repository;

import com.company.common.jpa.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 基礎 Repository 介面（向後相容）
 *
 * @deprecated 大部分實體不需要軟刪除，直接使用 {@code JpaRepository} 即可。
 *             需要軟刪除功能請改用 {@link SoftDeleteRepository}。
 */
@Deprecated
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID>
        extends SoftDeleteRepository<T, ID> {
}
