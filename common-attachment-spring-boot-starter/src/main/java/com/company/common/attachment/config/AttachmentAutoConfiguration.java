package com.company.common.attachment.config;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.attachment.persistence.repository.AttachmentBlobRepository;
import com.company.common.attachment.persistence.repository.AttachmentRepository;
import com.company.common.attachment.processing.ImageProcessingService;
import com.company.common.attachment.security.AttachmentAccessPolicy;
import com.company.common.attachment.security.DefaultDenyAccessPolicy;
import com.company.common.attachment.storage.AttachmentStorageStrategy;
import com.company.common.attachment.storage.DatabaseBlobStorageStrategy;
import com.company.common.attachment.storage.FilesystemStorageStrategy;
import com.company.common.attachment.validation.AttachmentValidator;
import com.company.common.attachment.validation.FileSizeValidator;
import com.company.common.attachment.validation.MimeTypeValidator;
import com.company.common.attachment.validation.PathTraversalGuard;
import com.company.common.attachment.web.AttachmentController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Path;
import java.util.List;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
@EnableConfigurationProperties(AttachmentProperties.class)
@EntityScan(basePackages = "com.company.common.attachment.persistence.entity")
@EnableJpaRepositories(basePackages = "com.company.common.attachment.persistence.repository")
@EnableAsync
public class AttachmentAutoConfiguration {

    // ========== Validators ==========

    @Bean
    @ConditionalOnMissingBean(PathTraversalGuard.class)
    public PathTraversalGuard pathTraversalGuard() {
        return new PathTraversalGuard();
    }

    @Bean
    @ConditionalOnMissingBean(FileSizeValidator.class)
    public FileSizeValidator fileSizeValidator(AttachmentProperties properties) {
        return new FileSizeValidator(properties);
    }

    @Bean
    @ConditionalOnMissingBean(MimeTypeValidator.class)
    public MimeTypeValidator mimeTypeValidator(AttachmentProperties properties) {
        return new MimeTypeValidator(properties);
    }

    // ========== Storage Strategy ==========

    @Bean
    @ConditionalOnMissingBean(AttachmentStorageStrategy.class)
    @ConditionalOnProperty(name = "wez.attachment.storage-type", havingValue = "DATABASE")
    public AttachmentStorageStrategy databaseBlobStorageStrategy(
            AttachmentBlobRepository blobRepository) {
        return new DatabaseBlobStorageStrategy(blobRepository);
    }

    @Bean
    @ConditionalOnMissingBean(AttachmentStorageStrategy.class)
    public AttachmentStorageStrategy filesystemStorageStrategy(AttachmentProperties properties) {
        return new FilesystemStorageStrategy(Path.of(properties.getStoragePath()).toAbsolutePath().normalize());
    }

    // ========== Security ==========

    @Bean
    @ConditionalOnMissingBean(AttachmentAccessPolicy.class)
    public AttachmentAccessPolicy defaultDenyAccessPolicy() {
        return new DefaultDenyAccessPolicy();
    }

    // ========== Core Service ==========

    @Bean
    @ConditionalOnMissingBean(AttachmentService.class)
    public AttachmentService attachmentService(
            AttachmentRepository attachmentRepository,
            AttachmentStorageStrategy storageStrategy,
            AttachmentAccessPolicy accessPolicy,
            List<AttachmentValidator> validators,
            ApplicationEventPublisher eventPublisher,
            AttachmentProperties properties) {
        return new AttachmentService(
                attachmentRepository, storageStrategy, accessPolicy,
                validators, eventPublisher, properties);
    }

    // ========== Image Processing ==========

    @Bean
    @ConditionalOnMissingBean(ImageProcessingService.class)
    public ImageProcessingService imageProcessingService(
            AttachmentStorageStrategy storageStrategy,
            AttachmentProperties properties) {
        return new ImageProcessingService(storageStrategy, properties);
    }

    // ========== Web Controller (optional) ==========

    @Bean
    @ConditionalOnMissingBean(AttachmentController.class)
    @ConditionalOnProperty(name = "wez.attachment.web.enabled", havingValue = "true")
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    public AttachmentController attachmentController(AttachmentService attachmentService) {
        return new AttachmentController(attachmentService);
    }
}
