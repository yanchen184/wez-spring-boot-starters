package com.company.common.attachment.storage;

public record StorageResult(
        String storedFilename,
        StorageType storageType,
        long fileSize
) {
}
