package com.company.common.attachment.storage;

import java.io.IOException;
import java.io.InputStream;

public interface AttachmentStorageStrategy {

    StorageResult store(String originalFilename, InputStream inputStream) throws IOException;

    InputStream load(String storedFilename) throws IOException;

    void delete(String storedFilename) throws IOException;

    StorageType getStorageType();
}
