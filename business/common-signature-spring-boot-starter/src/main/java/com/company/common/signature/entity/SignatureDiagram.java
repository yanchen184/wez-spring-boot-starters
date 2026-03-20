package com.company.common.signature.entity;

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

/**
 * 電子簽名資料。
 *
 * <p>每筆簽名透過 {@code ownerType + ownerId} 組合鍵與業務資料關聯。
 * 重簽時舊的整筆軟刪除（content + 附件都保留），新建一筆。
 * 繼承 BaseEntity 提供軟刪除（deleted）+ 樂觀鎖（version）+ 審計欄位。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "SIGNATURE_DIAGRAM", indexes = {
        @Index(name = "idx_sign_owner", columnList = "OWNER_TYPE, OWNER_ID")
})
public class SignatureDiagram extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long id;

    @Column(name = "OWNER_TYPE", nullable = false, length = 50)
    private String ownerType;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    /** Fabric.js Canvas JSON 序列化內容。 */
    @Column(name = "CONTENT", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    /** 關聯的附件 ID（簽名截圖 PNG）。 */
    @Column(name = "ATTACHMENT_ID")
    private Long attachmentId;
}
