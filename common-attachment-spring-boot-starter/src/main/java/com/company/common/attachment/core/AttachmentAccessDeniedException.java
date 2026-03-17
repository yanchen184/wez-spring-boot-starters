package com.company.common.attachment.core;

public class AttachmentAccessDeniedException extends RuntimeException {

    public AttachmentAccessDeniedException(Long id, String action) {
        super(String.format("無權%s附件: id=%d", action, id));
    }
}
