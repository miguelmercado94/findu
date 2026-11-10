package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.port.out.externalapi.NotificationSenderPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PushNotificationTemplateTest {

    private final NotificationSenderPort sender = mock(NotificationSenderPort.class);
    private final TemplateService templateService = mock(TemplateService.class);
    private final PushNotificationTemplate template = new PushNotificationTemplate(sender, templateService);

    private NotificationMessage msg(String code, Map<String, String> params) {
        return NotificationMessage.builder().notificationId("n1").type("PUSH").recipient("PROVEEDOR_7")
                .templateCode(code).language("es").params(params).build();
    }

    @Test
    @DisplayName("EVENT_PUSH usa pushTitle/pushBody y no consulta plantillas")
    void eventPush_usesParams() {
        when(sender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(NotificationResult.builder().success(true).build());

        NotificationResult r = template.execute(msg("EVENT_PUSH", Map.of("pushTitle", "Titulo", "pushBody", "Cuerpo")))
                .block();

        assertThat(r).isNotNull();
        assertThat(r.isSuccess()).isTrue();
        verify(sender).send(org.mockito.ArgumentMatchers.eq("PROVEEDOR_7"), org.mockito.ArgumentMatchers.eq("Titulo"),
                org.mockito.ArgumentMatchers.eq("Cuerpo"), any());
        verifyNoInteractions(templateService);
    }

    @Test
    @DisplayName("EVENT_PUSH sin params usa los defaults")
    void eventPush_defaults() {
        when(sender.send(anyString(), anyString(), anyString(), any()))
                .thenReturn(NotificationResult.builder().success(true).build());

        template.execute(msg("EVENT_PUSH", null)).block();

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sender).send(anyString(), title.capture(), body.capture(), any());
        assertThat(title.getValue()).isEqualTo("FindU");
        assertThat(body.getValue()).isEqualTo("Tienes una novedad en tu cuenta.");
    }

    @Test
    @DisplayName("TICKET_MESSAGE_RECEIVED sin cambios")
    void ticketMessage_unchanged() {
        when(sender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(NotificationResult.builder().success(true).build());

        template.execute(msg("TICKET_MESSAGE_RECEIVED",
                Map.of("ticketCode", "HT-1", "senderName", "Ana", "mensaje", "hola"))).block();

        verify(sender).send(org.mockito.ArgumentMatchers.eq("PROVEEDOR_7"),
                org.mockito.ArgumentMatchers.eq("Soporte FindU - Ticket #HT-1"),
                org.mockito.ArgumentMatchers.eq("Ana: hola"), anyMap());
    }
}
