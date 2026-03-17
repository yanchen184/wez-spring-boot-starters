package com.company.common.attachment.event;

import com.company.common.attachment.persistence.entity.AttachmentEntity;
import org.springframework.context.ApplicationEvent;

public class AttachmentDeletedEvent extends ApplicationEvent {

    private final AttachmentEntity attachment;

    public AttachmentDeletedEvent(Object source, AttachmentEntity attachment) {
        super(source);
        this.attachment = attachment;
    }

    public AttachmentEntity getAttachment() {
        return attachment;
    }
}
