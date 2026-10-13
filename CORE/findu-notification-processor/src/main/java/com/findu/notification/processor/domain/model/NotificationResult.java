package com.findu.notification.processor.domain.model;

import com.findu.notification.processor.domain.enums.NotificationChannel;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResult {
    private boolean success;
    private String notificationId;
    private NotificationChannel channel;
    private String recipient;
    private String error;
    private int retries;
}
