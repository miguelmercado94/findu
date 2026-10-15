package com.findu.notification.processor.application.service;

import com.findu.notification.processor.application.dto.event.PendingValidationAlertEvent;
import com.findu.notification.processor.application.dto.event.ValidationResolvedEvent;
import com.findu.notification.processor.application.port.in.ResolveTemplateUseCase;
import com.findu.notification.processor.application.port.out.AlertEventPublisherPort;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.AlertMeta;
import com.findu.notification.processor.domain.model.EventEnvelope;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import com.findu.notification.processor.domain.service.FinancialEventDeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    public static final String EVENT_TYPE_SYSTEM_ALERT = "SYSTEM_ALERT";
    public static final String ACTION_ALERT_UPSERT = "ALERT_UPSERT";
    public static final String ACTION_ALERT_DISMISSED = "ALERT_DISMISSED";

    private final ResolveTemplateUseCase resolveTemplateUseCase;
    private final AlertEventPublisherPort alertEventPublisherPort;
    private final FinancialEventDeduplicationService deduplicationService;

    public void processPending(PendingValidationAlertEvent event) {
        if (event == null) {
            return;
        }

        if (event.eventId() != null && deduplicationService.isDuplicate(event.eventId())) {
            log.info("PendingValidationAlertEvent duplicado ignorado. eventId={}", event.eventId());
            return;
        }

        String recipient = resolveRecipient(event.recipient(), event.userId());
        Optional<NotificationTemplate> optTemplate = resolveTemplateUseCase.resolveActive(
                event.alertTemplateCode(), TemplateType.ALERTA, "es");

        NotificationTemplate template = optTemplate.orElse(null);
        AlertMeta meta = template != null ? template.getAlertMeta() : null;

        String codigo = (event.validationCode() != null && !event.validationCode().isBlank())
                ? event.validationCode()
                : (template != null && template.getCodigo() != null ? template.getCodigo() : event.alertTemplateCode());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", ACTION_ALERT_UPSERT);
        payload.put("alertUuid", event.alertUuid());
        payload.put("codigo", codigo);
        payload.put("categoria", meta != null && meta.getCategoria() != null ? meta.getCategoria().name() : "INFO");
        payload.put("titulo", meta != null && meta.getTitulo() != null ? meta.getTitulo() : (template != null ? template.getTitle() : "Alerta de Validación"));
        payload.put("body", template != null && template.getBodyTemplate() != null ? template.getBodyTemplate() : "");
        payload.put("ctaLabel", meta != null ? meta.getCtaLabel() : null);
        payload.put("ctaAction", meta != null ? meta.getCtaAction() : null);
        payload.put("prioridad", meta != null && meta.getPrioridad() != null ? meta.getPrioridad() : 1);
        payload.put("cerrable", meta != null ? meta.isCerrable() : true);
        payload.put("pantallaDestino", meta != null && meta.getPantallaDestino() != null ? meta.getPantallaDestino() : "home");

        String eventId = (event.eventId() != null && !event.eventId().isBlank()) ? event.eventId() : UUID.randomUUID().toString();
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                EVENT_TYPE_SYSTEM_ALERT,
                eventId,
                recipient,
                payload
        );

        alertEventPublisherPort.publish(envelope);
    }

    public void processResolved(ValidationResolvedEvent event) {
        if (event == null) {
            return;
        }

        if (event.eventId() != null && deduplicationService.isDuplicate(event.eventId())) {
            log.info("ValidationResolvedEvent duplicado ignorado. eventId={}", event.eventId());
            return;
        }

        String recipient = resolveRecipient(null, event.userId());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", ACTION_ALERT_DISMISSED);
        payload.put("alertUuid", event.alertUuid());
        payload.put("reason", event.resolution());
        if (event.validationCode() != null) {
            payload.put("codigo", event.validationCode());
        }

        String eventId = (event.eventId() != null && !event.eventId().isBlank()) ? event.eventId() : UUID.randomUUID().toString();
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                EVENT_TYPE_SYSTEM_ALERT,
                eventId,
                recipient,
                payload
        );

        alertEventPublisherPort.publish(envelope);
    }

    private String resolveRecipient(String recipient, Long userId) {
        if (recipient != null && !recipient.isBlank()) {
            return recipient;
        }
        return "PROVEEDOR_" + userId;
    }
}
