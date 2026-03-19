package com.company.common.notification.channel;

import com.company.common.notification.entity.NotificationLog;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Email notification channel.
 * Uses Spring Boot's {@link JavaMailSender} to deliver HTML emails.
 */
public class EmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    private final JavaMailSender mailSender;
    private final RecipientResolver recipientResolver;
    private final String fromAddress;

    public EmailChannel(JavaMailSender mailSender, RecipientResolver recipientResolver,
                        String fromAddress) {
        this.mailSender = mailSender;
        this.recipientResolver = recipientResolver;
        this.fromAddress = fromAddress;
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public void send(NotificationLog notification) throws Exception {
        String email = recipientResolver.resolveEmail(notification.getRecipientId());
        if (email == null || email.isBlank()) {
            log.warn("No email found for user {}, skipping email notification",
                    notification.getRecipientId());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(email);
        helper.setSubject(notification.getSubject());
        helper.setText(notification.getContent(), true);

        mailSender.send(message);
        log.debug("Email sent to {} (user {}): {}",
                email, notification.getRecipientId(), notification.getSubject());
    }
}
