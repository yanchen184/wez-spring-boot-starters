package com.company.common.notification.config;

import com.company.common.notification.channel.EmailChannel;
import com.company.common.notification.channel.NotificationChannel;
import com.company.common.notification.channel.RecipientResolver;
import com.company.common.notification.channel.WebSocketChannel;
import com.company.common.notification.repository.NotificationLogRepository;
import com.company.common.notification.service.NotificationScheduler;
import com.company.common.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.TemplateEngine;

import java.util.List;
import java.util.concurrent.Executor;

@AutoConfiguration
@EnableConfigurationProperties(NotificationProperties.class)
@ConditionalOnProperty(prefix = "common.notification", name = "enabled", matchIfMissing = true)
@EnableJpaRepositories(basePackages = "com.company.common.notification.repository")
@EntityScan(basePackages = "com.company.common.notification.entity")
@EnableAsync
@EnableScheduling
public class NotificationAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NotificationAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "notificationExecutor")
    public Executor notificationExecutor(NotificationProperties properties) {
        NotificationProperties.Async asyncProps = properties.getAsync();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProps.getCorePoolSize());
        executor.setMaxPoolSize(asyncProps.getMaxPoolSize());
        executor.setQueueCapacity(asyncProps.getQueueCapacity());
        executor.setThreadNamePrefix("notif-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("[Notification] Executor: core={}, max={}, queue={}",
                asyncProps.getCorePoolSize(), asyncProps.getMaxPoolSize(), asyncProps.getQueueCapacity());
        return executor;
    }

    // --- Channels ---

    @Bean
    @ConditionalOnMissingBean(EmailChannel.class)
    public EmailChannel emailChannel(
            org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider,
            org.springframework.beans.factory.ObjectProvider<RecipientResolver> recipientResolverProvider,
            NotificationProperties properties) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        RecipientResolver resolver = recipientResolverProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("[Notification] No JavaMailSender found, EmailChannel disabled. "
                    + "Add spring-boot-starter-mail + spring.mail.host to enable.");
            return null;
        }
        if (resolver == null) {
            log.warn("[Notification] No RecipientResolver found, EmailChannel disabled. "
                    + "Implement RecipientResolver to enable email notifications.");
            return null;
        }
        log.info("[Notification] Registering EmailChannel (from: {})", properties.getFromAddress());
        return new EmailChannel(mailSender, resolver, properties.getFromAddress());
    }

    @Bean
    @ConditionalOnMissingBean(WebSocketChannel.class)
    @ConditionalOnClass(SimpMessagingTemplate.class)
    @ConditionalOnBean(SimpMessagingTemplate.class)
    public WebSocketChannel webSocketChannel(SimpMessagingTemplate messagingTemplate) {
        log.info("[Notification] Registering WebSocketChannel");
        return new WebSocketChannel(messagingTemplate);
    }

    // --- Core services ---

    @Bean
    @ConditionalOnMissingBean
    public NotificationService notificationService(NotificationLogRepository logRepository,
                                                   List<NotificationChannel> channels,
                                                   TemplateEngine templateEngine,
                                                   NotificationProperties properties) {
        return new NotificationService(logRepository, channels,
                templateEngine, properties.getDefaultChannels());
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationScheduler notificationScheduler(NotificationLogRepository logRepository,
                                                       NotificationService notificationService,
                                                       NotificationProperties properties) {
        return new NotificationScheduler(logRepository, notificationService,
                properties.getMaxRetry(), properties.getRetentionDays());
    }
}
