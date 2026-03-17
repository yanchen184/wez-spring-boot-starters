package com.company.common.attachment.storage;

import com.company.common.attachment.persistence.entity.AttachmentBlobEntity;
import com.company.common.attachment.persistence.repository.AttachmentBlobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DatabaseBlobStorageStrategy implements AttachmentStorageStrategy {

    private final AttachmentBlobRepository blobRepository;

    @Override
    public StorageResult store(String originalFilename, InputStream inputStream) throws IOException {
        byte[] content = inputStream.readAllBytes();
        String storedFilename = UUID.randomUUID().toString();

        AttachmentBlobEntity blob = new AttachmentBlobEntity();
        blob.setStoredFilename(storedFilename);
        blob.setBlobContent(content);
        blobRepository.save(blob);

        log.info("檔案已儲存至資料庫: {} ({} bytes)", storedFilename, content.length);
        return new StorageResult(storedFilename, StorageType.DATABASE, content.length);
    }

    @Override
    public InputStream load(String storedFilename) throws IOException {
        AttachmentBlobEntity blob = blobRepository.findByStoredFilename(storedFilename)
                .orElseThrow(() -> new IOException("資料庫中找不到檔案: " + storedFilename));
        return new ByteArrayInputStream(blob.getBlobContent());
    }

    @Override
    public void delete(String storedFilename) throws IOException {
        int deleted = blobRepository.deleteByStoredFilename(storedFilename);
        if (deleted > 0) {
            log.info("檔案已從資料庫刪除: {}", storedFilename);
        } else {
            log.warn("資料庫中找不到檔案，跳過刪除: {}", storedFilename);
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.DATABASE;
    }
}
