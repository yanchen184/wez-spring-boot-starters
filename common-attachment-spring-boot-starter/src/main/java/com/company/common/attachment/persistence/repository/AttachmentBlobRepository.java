package com.company.common.attachment.persistence.repository;

import com.company.common.attachment.persistence.entity.AttachmentBlobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AttachmentBlobRepository extends JpaRepository<AttachmentBlobEntity, Long> {

    Optional<AttachmentBlobEntity> findByStoredFilename(String storedFilename);

    @Modifying
    @Transactional
    @Query("DELETE FROM AttachmentBlobEntity b WHERE b.storedFilename = :storedFilename")
    int deleteByStoredFilename(@Param("storedFilename") String storedFilename);
}
