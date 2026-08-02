package com.findu.notification.processor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO para que el frontend móvil registre su device token de Firebase.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRegistration {
    @NotBlank
    private String username;
    @NotBlank
    private String deviceToken;
    private String platform; // ANDROID, IOS, WEB
}
