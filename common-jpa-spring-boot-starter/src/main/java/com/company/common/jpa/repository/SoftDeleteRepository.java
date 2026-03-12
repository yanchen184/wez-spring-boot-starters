package com.company.common.jpa.repository;

import com.company.common.jpa.entity.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 軟刪除 Repository
 *
 * <p>僅適用於繼承 {@link BaseEntity}（含 deleted + version）的實體。
 * 大部分實體不需要軟刪除，直接使用 {@code JpaRepository} 即可。
 *
 * @param <T>  實體類別（必須繼承 BaseEntity）
 * @param <ID> 主鍵類型
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends BaseEntity, ID>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /** 查詢所有未刪除的資料 */
    @Transactional(readOnly = true)
    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    List<T> findAllActive();

    /** 查詢所有未刪除的資料（分頁） */
    @Transactional(readOnly = true)
    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    Page<T> findAllActive(Pageable pageable);

    /** 根據 ID 查詢未刪除的資料 */
    @Transactional(readOnly = true)
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    Optional<T> findByIdActive(@Param("id") ID id);

    /** 根據 ID 集合查詢未刪除的資料 */
    @Transactional(readOnly = true)
    @Query("SELECT e FROM #{#entityName} e WHERE e.id IN :ids AND e.deleted = false")
    List<T> findAllByIdActive(@Param("ids") Collection<ID> ids);

    /** 邏輯刪除（軟刪除） */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true WHERE e.id = :id")
    int softDeleteById(@Param("id") ID id);

    /** 批次邏輯刪除 */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true WHERE e.id IN :ids")
    int softDeleteByIds(@Param("ids") Collection<ID> ids);

    /** 恢復刪除 */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = false WHERE e.id = :id")
    int restoreById(@Param("id") ID id);

    /** 統計未刪除的資料數量 */
    @Transactional(readOnly = true)
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deleted = false")
    long countActive();

    /** 檢查 ID 是否存在且未刪除 */
    @Transactional(readOnly = true)
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    boolean existsByIdActive(@Param("id") ID id);
}
