package com.findu.notification.processor.presentation.controller;

import com.findu.notification.processor.dto.DeviceTokenRegistration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint para que los clientes móviles registren su device token de Firebase.
 * Necesario para enviar push notifications.
 */
@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Dispositivos", description = "Registro de tokens de dispositivo para push notifications")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    @PostMapping("/register")
    @Operation(summary = "Registrar device token", description = "El frontend móvil registra su token de Firebase para recibir push notifications")
    public ResponseEntity<Void> registerDevice(@Valid @RequestBody DeviceTokenRegistration registration) {
        // TODO: Persistir en BD (tabla device_tokens) para lookup cuando se envíe push
        log.info("Device token registered: username={} platform={} token={}...",
                registration.getUsername(), registration.getPlatform(),
                registration.getDeviceToken().substring(0, Math.min(20, registration.getDeviceToken().length())));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unregister")
    @Operation(summary = "Desregistrar device token", description = "Elimina el token del dispositivo (logout o desinstalación)")
    public ResponseEntity<Void> unregisterDevice(@RequestParam String username, @RequestParam String deviceToken) {
        log.info("Device token unregistered: username={}", username);
        return ResponseEntity.noContent().build();
    }
}
