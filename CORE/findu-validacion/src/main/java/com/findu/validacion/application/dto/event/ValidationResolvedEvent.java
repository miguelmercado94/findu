package com.findu.validacion.application.dto.event;

public record ValidationResolvedEvent(
        String eventId,
        Long userId,
        String validationCode,
        String alertUuid,
        String resolution
) {
}
