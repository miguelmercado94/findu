package com.findu.notification.processor.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.notification.processor.application.dto.event.PendingValidationAlertEvent;
import com.findu.notification.processor.application.dto.event.ValidationResolvedEvent;
import com.findu.notification.processor.application.port.in.ResolveTemplateUseCase;
import com.findu.notification.processor.application.port.out.AlertEventPublisherPort;
import com.findu.notification.processor.config.RabbitMqConfig;
import com.findu.notification.processor.domain.enums.AlertCategory;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.AlertMeta;
import com.findu.notification.processor.domain.model.EventEnvelope;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import com.findu.notification.processor.domain.service.FinancialEventDeduplicationService;
import com.findu.notification.processor.infrastructure.consumer.AlertEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AlertNotificationServiceTest {

    private ResolveTemplateUseCase resolveTemplateUseCase;
    private AlertEventPublisherPort alertEventPublisherPort;
    private FinancialEventDeduplicationService deduplicationService;
    private AlertNotificationService service;
    private AlertEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resolveTemplateUseCase = mock(ResolveTemplateUseCase.class);
        alertEventPublisherPort = mock(AlertEventPublisherPort.class);
        deduplicationService = new FinancialEventDeduplicationService();
        service = new AlertNotificationService(resolveTemplateUseCase, alertEventPublisherPort, deduplicationService);
        objectMapper = new ObjectMapper();
        consumer = new AlertEventConsumer(service, objectMapper);
    }

    @Test
    @DisplayName("processPending resuelve plantilla y publica EventEnvelope SYSTEM_ALERT con action ALERT_UPSERT")
    void processPending_success() {
        NotificationTemplate template = NotificationTemplate.builder()
                .codigo("BIOMETRIA_PENDIENTE")
                .tipo(TemplateType.ALERTA)
                .bodyTemplate("Por favor completa tu verificación biométrica")
                .alertMeta(AlertMeta.builder()
                        .categoria(AlertCategory.PREVENTIVA)
                        .titulo("Verificación Biométrica Requerida")
                        .ctaLabel("Validar ahora")
                        .ctaAction("findu://perfil/verificacion-biometrica")
                        .prioridad(1)
                        .cerrable(false)
                        .pantallaDestino("home")
                        .build())
                .build();

        when(resolveTemplateUseCase.resolveActive(eq("BIOMETRIA_PENDIENTE"), eq(TemplateType.ALERTA), eq("es")))
                .thenReturn(Optional.of(template));

        PendingValidationAlertEvent event = new PendingValidationAlertEvent(
                "evt-101",
                42L,
                "ACTIVACION_PROVEEDOR",
                "BIOMETRIA",
                "BIOMETRIA_PENDIENTE",
                "PROVEEDOR_42",
                "uuid-abc-123"
        );

        service.processPending(event);

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(alertEventPublisherPort).publish(captor.capture());

        EventEnvelope<?> captured = captor.getValue();
        assertThat(captured.eventType()).isEqualTo("SYSTEM_ALERT");
        assertThat(captured.eventId()).isEqualTo("evt-101");
        assertThat(captured.targetUserId()).isEqualTo("PROVEEDOR_42");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) captured.payload();
        assertThat(payload.get("action")).isEqualTo("ALERT_UPSERT");
        assertThat(payload.get("alertUuid")).isEqualTo("uuid-abc-123");
        assertThat(payload.get("codigo")).isEqualTo("BIOMETRIA");
        assertThat(payload.get("categoria")).isEqualTo("PREVENTIVA");
        assertThat(payload.get("titulo")).isEqualTo("Verificación Biométrica Requerida");
        assertThat(payload.get("body")).isEqualTo("Por favor completa tu verificación biométrica");
        assertThat(payload.get("ctaLabel")).isEqualTo("Validar ahora");
        assertThat(payload.get("ctaAction")).isEqualTo("findu://perfil/verificacion-biometrica");
        assertThat(payload.get("prioridad")).isEqualTo(1);
        assertThat(payload.get("cerrable")).isEqualTo(false);
        assertThat(payload.get("pantallaDestino")).isEqualTo("home");
    }

    @Test
    @DisplayName("processPending ignora eventos duplicados por eventId")
    void processPending_duplicateIgnored() {
        PendingValidationAlertEvent event = new PendingValidationAlertEvent(
                "evt-duplicate",
                42L,
                "ACTIVACION_PROVEEDOR",
                "BIOMETRIA",
                "BIOMETRIA_PENDIENTE",
                "PROVEEDOR_42",
                "uuid-abc-123"
        );

        service.processPending(event);
        service.processPending(event);

        verify(alertEventPublisherPort, times(1)).publish(any());
    }

    @Test
    @DisplayName("processResolved publica EventEnvelope SYSTEM_ALERT con action ALERT_DISMISSED")
    void processResolved_success() {
        ValidationResolvedEvent event = new ValidationResolvedEvent(
                "evt-202",
                42L,
                "BIOMETRIA",
                "uuid-abc-123",
                "APPROVED"
        );

        service.processResolved(event);

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(alertEventPublisherPort).publish(captor.capture());

        EventEnvelope<?> captured = captor.getValue();
        assertThat(captured.eventType()).isEqualTo("SYSTEM_ALERT");
        assertThat(captured.eventId()).isEqualTo("evt-202");
        assertThat(captured.targetUserId()).isEqualTo("PROVEEDOR_42");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) captured.payload();
        assertThat(payload.get("action")).isEqualTo("ALERT_DISMISSED");
        assertThat(payload.get("alertUuid")).isEqualTo("uuid-abc-123");
        assertThat(payload.get("reason")).isEqualTo("APPROVED");
        assertThat(payload.get("codigo")).isEqualTo("BIOMETRIA");
    }

    @Test
    @DisplayName("AlertEventConsumer consume mensaje AMQP de alert.pending correctamente")
    void consumer_consumesPendingMessage() throws Exception {
        PendingValidationAlertEvent pendingEvent = new PendingValidationAlertEvent(
                "evt-amqp-1",
                88L,
                "PROC",
                "VAL_CODE",
                "TPL_CODE",
                "PROVEEDOR_88",
                "uuid-999"
        );

        String json = objectMapper.writeValueAsString(pendingEvent);
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("alert.pending");
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);

        consumer.consumeAlertMessage(message);

        verify(alertEventPublisherPort).publish(any());
    }

    @Test
    @DisplayName("AlertEventConsumer consume mensaje AMQP de alert.resolved correctamente")
    void consumer_consumesResolvedMessage() throws Exception {
        ValidationResolvedEvent resolvedEvent = new ValidationResolvedEvent(
                "evt-amqp-2",
                88L,
                "VAL_CODE",
                "uuid-999",
                "REJECTED"
        );

        String json = objectMapper.writeValueAsString(resolvedEvent);
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("alert.resolved");
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);

        consumer.consumeAlertMessage(message);

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(alertEventPublisherPort).publish(captor.capture());

        EventEnvelope<?> captured = captor.getValue();
        assertThat(captured.targetUserId()).isEqualTo("PROVEEDOR_88");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) captured.payload();
        assertThat(payload.get("action")).isEqualTo("ALERT_DISMISSED");
    }

    @Test
    @DisplayName("jsonMessageConverter y AlertEventConsumer ignoran header __TypeId__ de clase inexistente del productor")
    void converter_and_consumer_ignoreForeignTypeIdHeader() throws Exception {
        ValidationResolvedEvent resolvedEvent = new ValidationResolvedEvent(
                "evt-amqp-foreign-type",
                99L,
                "BIOMETRIA",
                "uuid-foreign-1",
                "APPROVED"
        );

        String json = objectMapper.writeValueAsString(resolvedEvent);
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setReceivedRoutingKey("alert.resolved");
        props.setHeader("__TypeId__", "com.findu.validacion.application.dto.event.ValidationResolvedEvent");
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);

        Jackson2JsonMessageConverter converter = new RabbitMqConfig().jsonMessageConverter();

        assertThatCode(() -> {
            Object converted = converter.fromMessage(message);
            assertThat(converted).isNotNull();
        }).doesNotThrowAnyException();

        consumer.consumeAlertMessage(message);

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(alertEventPublisherPort).publish(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("evt-amqp-foreign-type");
        assertThat(captor.getValue().targetUserId()).isEqualTo("PROVEEDOR_99");
    }
}
