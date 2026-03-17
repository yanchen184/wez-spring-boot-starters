package com.company.common.attachment.security;

import com.company.common.attachment.persistence.entity.AttachmentEntity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultDenyAccessPolicy implements AttachmentAccessPolicy {

    @Override
    public boolean canAccess(AttachmentEntity attachment) {
        log.warn("使用預設拒絕策略，拒絕存取附件 id={}。請實作 AttachmentAccessPolicy 並註冊為 Bean。",
                attachment.getId());
        return false;
    }

    @Override
    public boolean canDelete(AttachmentEntity attachment) {
        log.warn("使用預設拒絕策略，拒絕刪除附件 id={}。請實作 AttachmentAccessPolicy 並註冊為 Bean。",
                attachment.getId());
        return false;
    }
}
