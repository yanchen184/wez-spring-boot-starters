package com.company.common.attachment.core;

public class AttachmentNotFoundException extends RuntimeException {

    public AttachmentNotFoundException(Long id) {
        super("找不到附件: id=" + id);
    }
}
