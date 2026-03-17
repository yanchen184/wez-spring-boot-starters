package com.company.common.attachment.persistence.entity;

import com.company.common.attachment.storage.StorageType;
import com.company.common.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "attachment", indexes = {
        @Index(name = "idx_attachment_owner", columnList = "owner_type, owner_id"),
        @Index(name = "idx_attachment_stored_filename", columnList = "stored_filename")
})
public class AttachmentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ownerType;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 500)
    private String originalFilename;

    @Column(nullable = false, length = 255)
    private String storedFilename;

    @Column(length = 20)
    private String extension;

    @Column(length = 500)
    private String displayName;

    @Column(nullable = false, length = 255)
    private String mimeType;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StorageType storageType;
}
