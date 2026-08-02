package com.findu.notification.model;

import lombok.*;

/**
 * Respuesta de la Lambda.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchResult {
    private boolean success;
    private String message;
    private String notificationId;
}
