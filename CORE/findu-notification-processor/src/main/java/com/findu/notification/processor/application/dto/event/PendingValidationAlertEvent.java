package com.findu.notification.processor.application.dto.event;

public record PendingValidationAlertEvent(
        String eventId,
        Long userId,
        String processCode,
        String validationCode,
        String alertTemplateCode,
        String recipient,
        String alertUuid
) {
}
