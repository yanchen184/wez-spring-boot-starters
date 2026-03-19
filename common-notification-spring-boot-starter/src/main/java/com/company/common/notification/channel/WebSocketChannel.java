package com.company.common.notification.channel;

import com.company.common.notification.entity.NotificationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

/**
 * WebSocket STOMP notification channel.
 * Sends messages to individual user destinations via Spring Messaging.
 */
public class WebSocketChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannel.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChannel(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public String getChannelName() {
        return "WEBSOCKET";
    }

    @Override
    public void send(NotificationLog notification) throws Exception {
        String destination = "/queue/notifications";
        String userId = notification.getRecipientId().toString();

        Map<String, Object> payload = Map.of(
                "id", notification.getId(),
                "subject", notification.getSubject(),
                "content", notification.getContent() != null ? notification.getContent() : "",
                "category", notification.getCategory() != null ? notification.getCategory() : "",
                "timestamp", notification.getCreatedDate() != null
                        ? notification.getCreatedDate().toString() : ""
        );

        messagingTemplate.convertAndSendToUser(userId, destination, payload);
        log.debug("WebSocket notification sent to user {}: {}", userId, notification.getSubject());
    }
}
