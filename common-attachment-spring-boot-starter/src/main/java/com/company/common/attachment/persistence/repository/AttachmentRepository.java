package com.company.common.attachment.persistence.repository;

import com.company.common.attachment.persistence.entity.AttachmentEntity;
import com.company.common.jpa.repository.SoftDeleteRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttachmentRepository extends SoftDeleteRepository<AttachmentEntity, Long> {

    @Query("SELECT a FROM AttachmentEntity a WHERE a.ownerType = :ownerType "
            + "AND a.ownerId = :ownerId AND a.deleted = false ORDER BY a.createdDate DESC")
    List<AttachmentEntity> findByOwner(
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId
    );
}
