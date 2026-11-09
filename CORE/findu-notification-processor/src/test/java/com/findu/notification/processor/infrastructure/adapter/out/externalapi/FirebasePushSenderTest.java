package com.findu.notification.processor.infrastructure.adapter.out.externalapi;

import com.findu.notification.processor.application.port.out.persistence.DeviceTokenRepositoryPort;
import com.findu.notification.processor.domain.model.NotificationResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebasePushSenderTest {

    @Mock
    private FirebaseMessaging messaging;

    @Mock
    private DeviceTokenRepositoryPort deviceTokenRepository;

    private static final String RECIPIENT = "PROVEEDOR_42";

    private Map<String, String> metadata() {
        Map<String, String> meta = new HashMap<>();
        meta.put("type", "TICKET_CHAT");
        meta.put("ticketCode", "HT-8899");
        meta.put("senderName", "Soporte FindU");
        meta.put("mensaje", "Tu ticket tiene una respuesta");
        return meta;
    }

    @Test
    @DisplayName("(a) 1 token -> envía 1 mensaje con data type=TICKET_CHAT + ticketCode")
    void send_oneToken_sendsMessageWithChatData() throws Exception {
        FirebasePushSender sender = new FirebasePushSender(messaging, deviceTokenRepository);
        when(deviceTokenRepository.findTokensByCanonicalRecipient(RECIPIENT))
                .thenReturn(List.of("tok-aaaaaaaaaaaa-1111"));

        NotificationResult result =
                sender.send(RECIPIENT, "Nuevo mensaje", "Tienes una respuesta", metadata());

        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messaging, times(1)).send(captor.capture());

        Map<String, String> data = dataOf(captor.getValue());
        assertThat(data).containsEntry("type", "TICKET_CHAT");
        assertThat(data).containsEntry("ticketCode", "HT-8899");
    }

    @Test
    @DisplayName("(b) N tokens -> N envíos")
    void send_nTokens_sendsNMessages() throws Exception {
        FirebasePushSender sender = new FirebasePushSender(messaging, deviceTokenRepository);
        when(deviceTokenRepository.findTokensByCanonicalRecipient(RECIPIENT))
                .thenReturn(List.of("tok-1111aaaa", "tok-2222bbbb", "tok-3333cccc"));

        NotificationResult result =
                sender.send(RECIPIENT, "Nuevo mensaje", "cuerpo", metadata());

        assertThat(result.isSuccess()).isTrue();
        verify(messaging, times(3)).send(any(Message.class));
    }

    @Test
    @DisplayName("(c) sin tokens -> no-op success=true, 0 envíos")
    void send_noTokens_noOpSuccess() throws Exception {
        FirebasePushSender sender = new FirebasePushSender(messaging, deviceTokenRepository);
        when(deviceTokenRepository.findTokensByCanonicalRecipient(RECIPIENT))
                .thenReturn(List.of());

        NotificationResult result =
                sender.send(RECIPIENT, "Nuevo mensaje", "cuerpo", metadata());

        assertThat(result.isSuccess()).isTrue();
        verify(messaging, never()).send(any(Message.class));
    }

    @Test
    @DisplayName("(d) FirebaseMessagingException UNREGISTERED -> deleteByToken y no se propaga")
    void send_unregisteredToken_deletesTokenAndDoesNotThrow() throws Exception {
        FirebasePushSender sender = new FirebasePushSender(messaging, deviceTokenRepository);
        String badToken = "tok-dead-beef-9999";
        when(deviceTokenRepository.findTokensByCanonicalRecipient(RECIPIENT))
                .thenReturn(List.of(badToken));

        FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
        when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(messaging.send(any(Message.class))).thenThrow(ex);

        NotificationResult result =
                sender.send(RECIPIENT, "Nuevo mensaje", "cuerpo", metadata());

        // único token eliminado -> no hubo fallos no-token -> success=true (no-op efectivo)
        assertThat(result.isSuccess()).isTrue();
        verify(deviceTokenRepository, times(1)).deleteByToken(badToken);
    }

    @Test
    @DisplayName("(e) messaging null (disabled) -> no-op success=true, sin consultar repo")
    void send_messagingDisabled_noOpSuccess() {
        FirebasePushSender sender = new FirebasePushSender(null, deviceTokenRepository);

        NotificationResult result =
                sender.send(RECIPIENT, "Nuevo mensaje", "cuerpo", metadata());

        assertThat(result.isSuccess()).isTrue();
        verifyNoInteractions(deviceTokenRepository);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> dataOf(Message message) throws Exception {
        var field = Message.class.getDeclaredField("data");
        field.setAccessible(true);
        return (Map<String, String>) field.get(message);
    }
}
