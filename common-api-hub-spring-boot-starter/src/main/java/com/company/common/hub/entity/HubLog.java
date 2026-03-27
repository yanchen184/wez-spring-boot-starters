package com.company.common.hub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 呼叫日誌實體。
 *
 * <p>記錄每次 API 呼叫的請求資訊、回應結果與耗時。
 * 不繼承 AuditableEntity，僅保留 createdAt 欄位。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "HUB_LOG")
public class HubLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long id;

    /** 關聯授權設定（nullable，失敗時可能為空）。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HUB_USER_SET_ID")
    private HubUserSet hubUserSet;

    /** HTTP 方法（GET/POST/PUT/DELETE）。 */
    @Column(name = "REQUEST_METHOD", length = 10)
    private String requestMethod;

    /** 請求 URI。 */
    @Column(name = "REQUEST_URI", length = 1000)
    private String requestUri;

    /** 請求參數（密碼已脫敏）。 */
    @Column(name = "REQUEST_PARAMS", columnDefinition = "TEXT")
    private String requestParams;

    /** 客戶端 IP。 */
    @Column(name = "IP", length = 100)
    private String ip;

    /** 是否成功。 */
    @Column(name = "SUCCESS")
    private Boolean success;

    /** 回傳代碼。 */
    @Column(name = "RESPONSE_CODE", length = 10)
    private String responseCode;

    /** 回傳內容。 */
    @Column(name = "RESPONSE_RESULT", columnDefinition = "TEXT")
    private String responseResult;

    /** 錯誤訊息。 */
    @Column(name = "ERROR_LOG", columnDefinition = "TEXT")
    private String errorLog;

    /** 耗時（毫秒）。 */
    @Column(name = "ELAPSED_MS")
    private Long elapsedMs;

    /** 建立時間（自動填入，不可更新）。 */
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
