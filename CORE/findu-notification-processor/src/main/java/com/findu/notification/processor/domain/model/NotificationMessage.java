package com.findu.notification.processor.domain.model;

import lombok.*;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationMessage {
    private String notificationId;
    private String type;
    private String recipient;
    private String templateCode;
    private String language;
    private Map<String, String> params;
    private boolean requiresConnection;
    private String createdAt;
}
