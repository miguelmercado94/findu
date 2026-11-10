package com.findu.notification.processor.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.notification.processor.domain.model.NotificationMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackPushMapperTest {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final String DEFAULTS = FallbackPushMapper.DEFAULT_SERVICE_UPDATE_TYPES;

    private final FallbackPushMapper mapper = new FallbackPushMapper(false, DEFAULTS);

    private static JsonNode json(String s) throws Exception {
        return OM.readTree(s);
    }

    @Test
    @DisplayName("chat: salida identica a la historica (TICKET_MESSAGE_RECEIVED / TICKET_CHAT)")
    void chat_isUnchanged() throws Exception {
        Optional<NotificationMessage> out = mapper.map(json("""
                {"eventType":"CHAT_MESSAGE","targetUserId":"PROVEEDOR_7","payload":{"ticketCode":"HT-1","content":"hola","senderName":"Ana"}}
                """));
        assertThat(out).isPresent();
        NotificationMessage m = out.get();
        assertThat(m.getTemplateCode()).isEqualTo("TICKET_MESSAGE_RECEIVED");
        assertThat(m.getType()).isEqualTo("PUSH");
        assertThat(m.getRecipient()).isEqualTo("PROVEEDOR_7");
        assertThat(m.getParams()).containsEntry("type", "TICKET_CHAT").containsEntry("ticketCode", "HT-1")
                .containsEntry("mensaje", "hola").containsEntry("senderName", "Ana");
        assertThat(m.isRequiresConnection()).isFalse();
    }

    @Test
    @DisplayName("chat: defaults N/A y Soporte FindU; heuristica por payload.content sin eventType")
    void chat_defaultsAndHeuristic() throws Exception {
        NotificationMessage m = mapper.map(json("""
                {"targetUserId":"PROVEEDOR_7","payload":{"content":"x"}}
                """)).orElseThrow();
        assertThat(m.getParams()).containsEntry("ticketCode", "N/A").containsEntry("senderName", "Soporte FindU");
    }

    @Test
    @DisplayName("chat: eventId se propaga como campo aditivo")
    void chat_eventIdAdded() throws Exception {
        NotificationMessage m = mapper.map(json("""
                {"eventType":"CHAT_MESSAGE","eventId":"e-1","targetUserId":"PROVEEDOR_7","payload":{"content":"x"}}
                """)).orElseThrow();
        assertThat(m.getParams()).containsEntry("eventId", "e-1");
    }

    @Test
    @DisplayName("OFFER_RECEIVED a proveedor sin titulo/cuerpo: plantilla por defecto")
    void offerReceived_defaults() throws Exception {
        NotificationMessage m = mapper.map(json("""
                {"eventType":"OFFER_RECEIVED","eventId":"e-2","targetUserId":"PROVEEDOR_7","payload":{"solicitudId":10,"ofertaId":"3"}}
                """)).orElseThrow();
        assertThat(m.getTemplateCode()).isEqualTo("EVENT_PUSH");
        assertThat(m.getLanguage()).isEqualTo("es");
        assertThat(m.getParams()).containsEntry("type", "OFFER_RECEIVED").containsEntry("solicitudId", "10")
                .containsEntry("ofertaId", "3").containsEntry("eventId", "e-2")
                .containsEntry("pushTitle", "Recibiste una nueva oferta")
                .containsEntry("pushBody", "Revisa los detalles en la app.");
    }

    @Test
    @DisplayName("OFFER_RECEIVED con titulo/mensaje del payload los usa")
    void offerReceived_payloadTexts() throws Exception {
        NotificationMessage m = mapper.map(json("""
                {"eventType":"OFFER_RECEIVED","targetUserId":"PROVEEDOR_7","payload":{"titulo":"T","mensaje":"M"}}
                """)).orElseThrow();
        assertThat(m.getParams()).containsEntry("pushTitle", "T").containsEntry("pushBody", "M");
    }

    @Test
    @DisplayName("review #1: targetUserId CLIENTE_5 (core emite OFFER_RECEIVED al cliente) -> vacio")
    void offerReceived_toCliente_isEmpty() throws Exception {
        assertThat(mapper.map(json("""
                {"eventType":"OFFER_RECEIVED","targetUserId":"CLIENTE_5","payload":{"solicitudId":10}}
                """))).isEmpty();
        assertThat(mapper.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"CLIENTE_5","payload":{"tipoEvento":"OFERTA_ACEPTADA"}}
                """))).isEmpty();
    }

    @Test
    @DisplayName("SERVICE_UPDATE permitido genera push con tipoEvento y plantilla")
    void serviceUpdate_allowed() throws Exception {
        NotificationMessage m = mapper.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":{"tipoEvento":"OFERTA_ACEPTADA","servicioNombre":"Plomeria","solicitudId":4}}
                """)).orElseThrow();
        assertThat(m.getParams()).containsEntry("type", "SERVICE_UPDATE").containsEntry("tipoEvento", "OFERTA_ACEPTADA")
                .containsEntry("pushTitle", "¡Oferta aceptada!")
                .containsEntry("pushBody", "Tu oferta para Plomeria fue aceptada. Revisa Mis Trabajos.");
    }

    @Test
    @DisplayName("NUEVA_SOLICITUD usa servicio + municipio")
    void nuevaSolicitud_body() throws Exception {
        NotificationMessage m = mapper.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":{"tipoEvento":"NUEVA_SOLICITUD","servicioNombre":"Plomeria","municipioNombre":"Cali"}}
                """)).orElseThrow();
        assertThat(m.getParams()).containsEntry("pushTitle", "Nueva solicitud disponible")
                .containsEntry("pushBody", "Plomeria en Cali");
    }

    @Test
    @DisplayName("SERVICE_UPDATE de disipacion (SOLICITUD_CERRADA) no genera push")
    void serviceUpdate_silent() throws Exception {
        assertThat(mapper.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":{"tipoEvento":"SOLICITUD_CERRADA"}}
                """))).isEmpty();
    }

    @Test
    @DisplayName("tipoEvento tambien se lee de action y tipo, sin importar mayusculas")
    void serviceUpdate_actionAndTipo() throws Exception {
        assertThat(mapper.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":{"action":"oferta_aceptada"}}
                """))).isPresent();
        assertThat(mapper.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":{"tipo":"NUEVA_SOLICITUD"}}
                """))).isPresent();
    }

    @Test
    @DisplayName("SYSTEM_ALERT: omitido con flag off, push con flag on")
    void systemAlert_flag() throws Exception {
        String ev = """
                {"eventType":"SYSTEM_ALERT","targetUserId":"PROVEEDOR_7","payload":{"id":1}}
                """;
        assertThat(mapper.map(json(ev))).isEmpty();
        NotificationMessage m = new FallbackPushMapper(true, DEFAULTS).map(json(ev)).orElseThrow();
        assertThat(m.getParams()).containsEntry("type", "SYSTEM_ALERT")
                .containsEntry("pushTitle", "Tienes una alerta pendiente");
    }

    @Test
    @DisplayName("titulo y cuerpo se truncan a 65 y 180 con elipsis")
    void truncation() throws Exception {
        String title = "t".repeat(100);
        String body = "b".repeat(300);
        NotificationMessage m = mapper.map(json("{\"eventType\":\"OFFER_RECEIVED\",\"targetUserId\":\"PROVEEDOR_7\","
                + "\"payload\":{\"titulo\":\"" + title + "\",\"mensaje\":\"" + body + "\"}}")).orElseThrow();
        assertThat(m.getParams().get("pushTitle")).hasSize(65).endsWith("…");
        assertThat(m.getParams().get("pushBody")).hasSize(180).endsWith("…");
    }

    @Test
    @DisplayName("payload ausente o no-objeto se trata como vacio y no lanza")
    void payloadNullOrScalar() throws Exception {
        assertThat(mapper.map(json("""
                {"eventType":"OFFER_RECEIVED","targetUserId":"PROVEEDOR_7"}
                """))).isPresent();
        assertThat(mapper.map(json("""
                {"eventType":"OFFER_RECEIVED","targetUserId":"PROVEEDOR_7","payload":"texto"}
                """))).isPresent();
        assertThat(mapper.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":null}
                """))).isEmpty();
    }

    @Test
    @DisplayName("titulo no escalar en el payload se ignora")
    void nonScalarTitleIgnored() throws Exception {
        NotificationMessage m = mapper.map(json("""
                {"eventType":"OFFER_RECEIVED","targetUserId":"PROVEEDOR_7","payload":{"titulo":{"a":1},"mensaje":["x"]}}
                """)).orElseThrow();
        assertThat(m.getParams()).containsEntry("pushTitle", "Recibiste una nueva oferta");
    }

    @Test
    @DisplayName("targetUserId vacio -> vacio; eventType desconocido -> vacio")
    void blankTargetAndUnknownType() throws Exception {
        assertThat(mapper.map(json("""
                {"eventType":"OFFER_RECEIVED","targetUserId":" ","payload":{}}
                """))).isEmpty();
        assertThat(mapper.map(json("""
                {"eventType":"OTRO","targetUserId":"PROVEEDOR_7","payload":{}}
                """))).isEmpty();
        assertThat(mapper.map(null)).isEmpty();
    }

    @Test
    @DisplayName("review #10: lista parseada con trim, mayusculas y vacios; vacia usa el default")
    void parseTypes() throws Exception {
        FallbackPushMapper custom = new FallbackPushMapper(false, " nueva_solicitud , ,x ");
        assertThat(custom.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":{"tipoEvento":"X"}}
                """))).isPresent();
        assertThat(custom.map(json("""
                {"eventType":"SERVICE_UPDATE","targetUserId":"PROVEEDOR_7","payload":{"tipoEvento":"OFERTA_ACEPTADA"}}
                """))).isEmpty();
        assertThat(FallbackPushMapper.parseTypes(" , ")).contains("OFERTA_ACEPTADA", "NUEVA_SOLICITUD");
        assertThat(FallbackPushMapper.parseTypes(null)).contains("OFERTA_ACEPTADA");
    }
}
