package com.company.common.attachment.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class FilesystemStorageStrategy implements AttachmentStorageStrategy {

    private final Path rootLocation;

    @Override
    public StorageResult store(String originalFilename, InputStream inputStream) throws IOException {
        Files.createDirectories(rootLocation);

        String extension = extractExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;
        Path targetPath = rootLocation.resolve(storedFilename).normalize();

        // 防止路徑穿越：確認最終路徑仍在 rootLocation 下
        if (!targetPath.startsWith(rootLocation)) {
            throw new IOException("無法將檔案儲存到根目錄之外: " + storedFilename);
        }

        long fileSize = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("檔案已儲存至檔案系統: {} ({} bytes)", storedFilename, fileSize);
        return new StorageResult(storedFilename, StorageType.FILESYSTEM, fileSize);
    }

    @Override
    public InputStream load(String storedFilename) throws IOException {
        Path filePath = rootLocation.resolve(storedFilename).normalize();
        if (!filePath.startsWith(rootLocation)) {
            throw new IOException("無法存取根目錄之外的檔案: " + storedFilename);
        }
        if (!Files.exists(filePath)) {
            throw new IOException("檔案不存在: " + storedFilename);
        }
        return Files.newInputStream(filePath);
    }

    @Override
    public void delete(String storedFilename) throws IOException {
        Path filePath = rootLocation.resolve(storedFilename).normalize();
        if (!filePath.startsWith(rootLocation)) {
            throw new IOException("無法刪除根目錄之外的檔案: " + storedFilename);
        }
        boolean deleted = Files.deleteIfExists(filePath);
        if (deleted) {
            log.info("檔案已從檔案系統刪除: {}", storedFilename);
        } else {
            log.warn("檔案不存在，跳過刪除: {}", storedFilename);
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.FILESYSTEM;
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
