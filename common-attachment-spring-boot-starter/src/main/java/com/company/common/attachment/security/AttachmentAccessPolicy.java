package com.company.common.attachment.security;

import com.company.common.attachment.persistence.entity.AttachmentEntity;

public interface AttachmentAccessPolicy {

    boolean canAccess(AttachmentEntity attachment);

    boolean canDelete(AttachmentEntity attachment);
}
