package com.company.common.attachment.event;

import com.company.common.attachment.persistence.entity.AttachmentEntity;
import org.springframework.context.ApplicationEvent;

public class AttachmentUploadedEvent extends ApplicationEvent {

    private final AttachmentEntity attachment;

    public AttachmentUploadedEvent(Object source, AttachmentEntity attachment) {
        super(source);
        this.attachment = attachment;
    }

    public AttachmentEntity getAttachment() {
        return attachment;
    }
}
