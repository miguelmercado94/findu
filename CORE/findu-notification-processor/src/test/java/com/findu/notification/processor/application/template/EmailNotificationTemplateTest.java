package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.port.out.externalapi.NotificationSenderPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.enums.NotificationChannel;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationTemplateTest {

    @Mock
    private NotificationSenderPort sender;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private EmailNotificationTemplate emailNotificationTemplate;

    @Test
    @DisplayName("Debe procesar TICKET_RESOLVED generando asunto y plantilla HTML con detalle de resolución")
    void execute_TicketResolved_GeneratesHtmlAndSends() {
        when(templateService.findTemplate(anyString(), any(TemplateType.class), anyString()))
                .thenReturn(Optional.empty());

        when(sender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(NotificationResult.builder()
                        .success(true)
                        .channel(NotificationChannel.EMAIL)
                        .recipient("cliente@findu.com")
                        .build());

        Map<String, String> params = new HashMap<>();
        params.put("ticketCode", "HT-8899");
        params.put("userName", "Carlos Proveedor");
        params.put("subject", "No puedo actualizar mi perfil");
        params.put("actionType", "SOLO_INFORMACION");
        params.put("agentName", "Agente Soporte");
        params.put("resolutionDetail", "Se limpiaron los permisos y la cuenta ya puede editar perfil.");

        NotificationMessage message = NotificationMessage.builder()
                .notificationId("notif-123")
                .type("EMAIL")
                .recipient("cliente@findu.com")
                .templateCode("TICKET_RESOLVED")
                .language("es")
                .params(params)
                .build();

        NotificationResult result = emailNotificationTemplate.execute(message).block();

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<String> recipientCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(sender, times(1)).send(
                recipientCaptor.capture(),
                subjectCaptor.capture(),
                bodyCaptor.capture(),
                anyMap()
        );

        assertThat(recipientCaptor.getValue()).isEqualTo("cliente@findu.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("FindU Soporte - Tu ticket #HT-8899 ha sido resuelto");
        assertThat(bodyCaptor.getValue()).contains("HT-8899");
        assertThat(bodyCaptor.getValue()).contains("Carlos Proveedor");
        assertThat(bodyCaptor.getValue()).contains("No puedo actualizar mi perfil");
        assertThat(bodyCaptor.getValue()).contains("Se limpiaron los permisos y la cuenta ya puede editar perfil.");
        assertThat(bodyCaptor.getValue()).contains("Caso Resuelto");
    }
}
