package com.findu.notification.processor.application.dto.event;

public record ValidationResolvedEvent(
        String eventId,
        Long userId,
        String validationCode,
        String alertUuid,
        String resolution
) {
}
