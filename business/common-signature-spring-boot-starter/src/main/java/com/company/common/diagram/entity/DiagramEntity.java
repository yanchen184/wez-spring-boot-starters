package com.company.common.diagram.entity;

import com.company.common.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "DIAGRAM", indexes = {
        @Index(name = "idx_diagram_owner", columnList = "DIAGRAM_TYPE, OWNER_TYPE, OWNER_ID")
})
public class DiagramEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long id;

    @Column(name = "DIAGRAM_TYPE", nullable = false, length = 30)
    private String diagramType;

    @Column(name = "OWNER_TYPE", nullable = false, length = 50)
    private String ownerType;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "NAME", length = 200)
    private String name;

    @Column(name = "CONTENT", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "ATTACHMENT_ID")
    private Long attachmentId;
}
