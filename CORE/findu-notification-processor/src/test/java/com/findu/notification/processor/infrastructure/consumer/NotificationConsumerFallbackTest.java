package com.findu.notification.processor.infrastructure.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.notification.processor.application.port.out.persistence.NotificationStatusRepositoryPort;
import com.findu.notification.processor.application.service.NotificationProcessorService;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerFallbackTest {

    @Mock
    private NotificationProcessorService processorService;
    @Mock
    private NotificationStatusRepositoryPort statusRepository;

    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(processorService, statusRepository, new ObjectMapper(),
                new FallbackPushMapper(false, FallbackPushMapper.DEFAULT_SERVICE_UPDATE_TYPES));
    }

    @Test
    @DisplayName("evento SERVICE_UPDATE permitido -> process con EVENT_PUSH")
    void serviceUpdate_isProcessed() {
        when(processorService.process(any(NotificationMessage.class)))
                .thenReturn(Mono.just(NotificationResult.builder().success(true).build()));

        consumer.handleFallback("""
                {"eventType":"SERVICE_UPDATE","eventId":"e1","targetUserId":"PROVEEDOR_7","payload":{"tipoEvento":"NUEVA_SOLICITUD","servicioNombre":"Plomeria"}}
                """);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(processorService).process(captor.capture());
        NotificationMessage m = captor.getValue();
        assertThat(m.getTemplateCode()).isEqualTo("EVENT_PUSH");
        assertThat(m.getRecipient()).isEqualTo("PROVEEDOR_7");
        assertThat(m.getParams()).containsEntry("type", "SERVICE_UPDATE").containsEntry("tipoEvento", "NUEVA_SOLICITUD");
        verify(statusRepository).saveSuccess(m.getNotificationId());
    }

    @Test
    @DisplayName("chat -> process con TICKET_MESSAGE_RECEIVED")
    void chat_isProcessed() {
        when(processorService.process(any(NotificationMessage.class)))
                .thenReturn(Mono.just(NotificationResult.builder().success(true).build()));

        consumer.handleFallback("""
                {"eventType":"CHAT_MESSAGE","targetUserId":"PROVEEDOR_7","payload":{"ticketCode":"HT-9","content":"hola"}}
                """);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(processorService).process(captor.capture());
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("TICKET_MESSAGE_RECEIVED");
        assertThat(captor.getValue().getParams()).containsEntry("ticketCode", "HT-9").containsEntry("mensaje", "hola");
    }

    @Test
    @DisplayName("evento no soportado o para CLIENTE -> no se procesa nada")
    void unsupported_isIgnored() {
        consumer.handleFallback("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":{"tipoEvento":"SOLICITUD_CERRADA"}}
                """);
        consumer.handleFallback("""
                {"eventType":"OFFER_RECEIVED","targetUserId":"CLIENTE_5","payload":{}}
                """);
        verify(processorService, never()).process(any());
        verifyNoInteractions(statusRepository);
    }

    @Test
    @DisplayName("JSON invalido o fallo del procesador no lanza (no se reencola)")
    void invalidJsonOrFailure_doesNotThrow() {
        assertThatCode(() -> consumer.handleFallback("{no es json")).doesNotThrowAnyException();
        assertThatCode(() -> consumer.handleFallback(null)).doesNotThrowAnyException();

        when(processorService.process(any(NotificationMessage.class)))
                .thenReturn(Mono.just(NotificationResult.builder().success(false).error("boom").retries(3).build()));
        assertThatCode(() -> consumer.handleFallback("""
                {"eventType":"OFFER_RECEIVED","targetUserId":"PROVEEDOR_7","payload":{}}
                """)).doesNotThrowAnyException();
    }
}
