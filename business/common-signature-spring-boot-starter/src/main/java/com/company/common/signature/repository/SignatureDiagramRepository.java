package com.company.common.signature.repository;

import com.company.common.jpa.repository.SoftDeleteRepository;
import com.company.common.signature.entity.SignatureDiagram;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignatureDiagramRepository extends SoftDeleteRepository<SignatureDiagram, Long> {

    /** 查詢未刪除的有效簽名 */
    @Query("SELECT s FROM SignatureDiagram s WHERE s.ownerType = :ownerType "
            + "AND s.ownerId = :ownerId AND s.deleted = false")
    Optional<SignatureDiagram> findActiveByOwner(
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId);

    /** 查詢是否存在未刪除的簽名 */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END "
            + "FROM SignatureDiagram s WHERE s.ownerType = :ownerType "
            + "AND s.ownerId = :ownerId AND s.deleted = false")
    boolean existsActiveByOwner(
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId);
}
