package com.company.common.signature.config;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.signature.repository.SignatureDiagramRepository;
import com.company.common.signature.service.SignatureService;
import com.company.common.signature.web.SignatureController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 電子簽名板模組自動配置
 *
 * <p>註冊 {@link SignatureService} 和 {@link SignatureController}，依賴 {@link AttachmentService}。
 */
@AutoConfiguration
@EnableConfigurationProperties(SignatureProperties.class)
@ConditionalOnProperty(prefix = "common.signature", name = "enabled", matchIfMissing = true)
@EntityScan(basePackages = "com.company.common.signature.entity")
@EnableJpaRepositories(basePackages = "com.company.common.signature.repository")
public class SignatureAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AttachmentService.class)
    public SignatureService signatureService(SignatureDiagramRepository repository,
                                             AttachmentService attachmentService) {
        return new SignatureService(repository, attachmentService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SignatureService.class)
    public SignatureController signatureController(SignatureService signatureService) {
        return new SignatureController(signatureService);
    }
}
