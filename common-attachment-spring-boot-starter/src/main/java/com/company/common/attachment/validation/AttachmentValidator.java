package com.company.common.attachment.validation;

import com.company.common.attachment.core.model.AttachmentUploadRequest;

public interface AttachmentValidator {

    void validate(AttachmentUploadRequest request);

    int getOrder();
}
