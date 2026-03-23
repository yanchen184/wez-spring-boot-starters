package com.company.common.diagram.config;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.diagram.repository.DiagramRepository;
import com.company.common.diagram.service.DiagramService;
import com.company.common.diagram.web.DiagramController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EnableConfigurationProperties(DiagramProperties.class)
@ConditionalOnProperty(prefix = "common.diagram", name = "enabled", matchIfMissing = true)
@EntityScan(basePackages = "com.company.common.diagram.entity")
@EnableJpaRepositories(basePackages = "com.company.common.diagram.repository")
public class DiagramAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AttachmentService.class)
    public DiagramService diagramService(DiagramRepository repository,
                                         AttachmentService attachmentService) {
        return new DiagramService(repository, attachmentService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DiagramService.class)
    @ConditionalOnProperty(name = "common.diagram.web.enabled", havingValue = "true")
    public DiagramController diagramController(DiagramService diagramService) {
        return new DiagramController(diagramService);
    }
}
