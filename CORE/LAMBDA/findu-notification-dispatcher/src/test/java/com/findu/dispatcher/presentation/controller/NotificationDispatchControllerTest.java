package com.findu.dispatcher.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.dispatcher.domain.model.EventEnvelope;
import com.findu.dispatcher.domain.model.TipoEventoCanal;
import com.findu.dispatcher.infrastructure.adapter.rabbitmq.TransactionalNotificationProducer;
import com.findu.dispatcher.presentation.controller.dto.NotificationMessagePayload;
import com.findu.dispatcher.service.EventDispatcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchControllerTest {

    @Mock
    private EventDispatcherService eventDispatcherService;

    @Mock
    private TransactionalNotificationProducer transactionalNotificationProducer;

    @InjectMocks
    private NotificationDispatchController controller;

    @Captor
    private ArgumentCaptor<NotificationMessagePayload> payloadCaptor;

    @Test
    void sms_con_templateCode_publica_al_productor_transaccional_y_no_al_servicio_de_presencia() throws Exception {
        when(transactionalNotificationProducer.publish(any())).thenReturn(Mono.empty());

        var request = new NotificationDispatchController.DispatchNotificationRequest(
                "SMS",
                "+573003763300",
                "VERIFICACION_OTP",
                "es",
                Map.of("otp_code", "123456", "expiration_minutes", 5)
        );

        ResponseEntity<Void> response = controller.dispatchNotification(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Ruta transaccional usada; presencia NO tocada.
        verify(transactionalNotificationProducer).publish(payloadCaptor.capture());
        verify(eventDispatcherService, never()).dispatch(anyString(), any(), any());

        NotificationMessagePayload payload = payloadCaptor.getValue();
        assertThat(payload.type()).isEqualTo("SMS");
        assertThat(payload.recipient()).isEqualTo("+573003763300");
        assertThat(payload.templateCode()).isEqualTo("VERIFICACION_OTP");
        assertThat(payload.language()).isEqualTo("es");
        assertThat(payload.requiresConnection()).isFalse();
        assertThat(payload.notificationId()).isNotBlank();
        assertThat(payload.createdAt()).isNotBlank();
        // params convertidos a Map<String,String>
        assertThat(payload.params()).containsEntry("otp_code", "123456");
        assertThat(payload.params()).containsEntry("expiration_minutes", "5");

        // El body JSON es deserializable por un POJO con el mismo contrato que NotificationMessage del processor.
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(payload);
        NotificationMessageContract deserialized = mapper.readValue(json, NotificationMessageContract.class);
        assertThat(deserialized.type).isEqualTo("SMS");
        assertThat(deserialized.recipient).isEqualTo("+573003763300");
        assertThat(deserialized.templateCode).isEqualTo("VERIFICACION_OTP");
        assertThat(deserialized.language).isEqualTo("es");
        assertThat(deserialized.params).containsEntry("otp_code", "123456");
        assertThat(deserialized.params).containsEntry("expiration_minutes", "5");
    }

    @Test
    void evento_sin_templateCode_sigue_la_ruta_de_presencia_y_no_el_productor_transaccional() {
        when(eventDispatcherService.dispatch(anyString(), any(EventEnvelope.class), any(TipoEventoCanal.class)))
                .thenReturn(Mono.empty());

        var request = new NotificationDispatchController.DispatchNotificationRequest(
                "OFFER_RECEIVED",
                "CLIENTE_42",
                null,
                "es",
                Map.of("ofertaId", "abc")
        );

        ResponseEntity<Void> response = controller.dispatchNotification(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        verify(eventDispatcherService).dispatch(anyString(), any(EventEnvelope.class), any(TipoEventoCanal.class));
        verify(transactionalNotificationProducer, never()).publish(any());
    }

    /**
     * Réplica mínima del contrato de deserialización del NotificationMessage del processor
     * (campos EXACTOS). Verifica que el JSON producido por el dispatcher mapea 1:1.
     */
    static class NotificationMessageContract {
        public String notificationId;
        public String type;
        public String recipient;
        public String templateCode;
        public String language;
        public Map<String, String> params;
        public boolean requiresConnection;
        public String createdAt;
    }
}
