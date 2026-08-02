package com.findu.notification.processor.domain.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResult {
    private boolean success;
    private String notificationId;
    private NotificationChannel channel;
    private String recipient;
    private String error;
}
