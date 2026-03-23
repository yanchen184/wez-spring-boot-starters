package com.company.common.diagram.repository;

import com.company.common.diagram.entity.DiagramEntity;
import com.company.common.jpa.repository.SoftDeleteRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagramRepository extends SoftDeleteRepository<DiagramEntity, Long> {

    @Query("SELECT d FROM DiagramEntity d WHERE d.diagramType = :diagramType "
            + "AND d.ownerType = :ownerType AND d.ownerId = :ownerId AND d.deleted = false")
    Optional<DiagramEntity> findActiveDiagram(
            @Param("diagramType") String diagramType,
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId);

    @Query("SELECT d FROM DiagramEntity d WHERE d.diagramType = :diagramType "
            + "AND d.ownerType = :ownerType AND d.ownerId = :ownerId "
            + "ORDER BY d.createdDate DESC")
    List<DiagramEntity> findAllVersions(
            @Param("diagramType") String diagramType,
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId);

    @Query("SELECT d FROM DiagramEntity d WHERE d.diagramType = :diagramType "
            + "AND d.ownerType = :ownerType AND d.ownerId = :ownerId AND d.deleted = false "
            + "ORDER BY d.createdDate DESC")
    List<DiagramEntity> findActiveByOwner(
            @Param("diagramType") String diagramType,
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId);
}
