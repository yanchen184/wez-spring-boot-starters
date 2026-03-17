package com.company.common.attachment.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesystemStorageStrategyTest {

    @TempDir
    Path tempDir;

    private FilesystemStorageStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FilesystemStorageStrategy(tempDir);
    }

    @Test
    @DisplayName("應能正確儲存並讀取檔案")
    void storeAndLoad_shouldWork() throws IOException {
        byte[] content = "Hello, attachment!".getBytes();
        StorageResult result = strategy.store("test.txt", new ByteArrayInputStream(content));

        assertThat(result.storageType()).isEqualTo(StorageType.FILESYSTEM);
        assertThat(result.fileSize()).isEqualTo(content.length);
        assertThat(result.storedFilename()).endsWith(".txt");

        try (InputStream loaded = strategy.load(result.storedFilename())) {
            assertThat(loaded.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    @DisplayName("刪除檔案後應無法再讀取")
    void deleteAndLoad_shouldThrow() throws IOException {
        byte[] content = "to be deleted".getBytes();
        StorageResult result = strategy.store("delete-me.txt", new ByteArrayInputStream(content));

        strategy.delete(result.storedFilename());

        assertThatThrownBy(() -> strategy.load(result.storedFilename()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("storage type 應為 FILESYSTEM")
    void getStorageType_shouldBeFilesystem() {
        assertThat(strategy.getStorageType()).isEqualTo(StorageType.FILESYSTEM);
    }
}
