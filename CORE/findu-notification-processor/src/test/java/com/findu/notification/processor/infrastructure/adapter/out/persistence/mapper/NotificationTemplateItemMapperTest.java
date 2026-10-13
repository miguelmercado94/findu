package com.findu.notification.processor.infrastructure.adapter.out.persistence.mapper;

import com.findu.notification.processor.domain.enums.AlertCategory;
import com.findu.notification.processor.domain.enums.AudienceMode;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.AlertMeta;
import com.findu.notification.processor.domain.model.Audience;
import com.findu.notification.processor.domain.model.NotificationRouting;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplateItemMapperTest {

    private final NotificationTemplateItemMapper mapper = new NotificationTemplateItemMapper();

    private NotificationTemplate.NotificationTemplateBuilder base(TemplateType tipo, String codigo) {
        return NotificationTemplate.builder()
                .id(codigo + "#" + tipo.name() + "#es")
                .sk("v#ACTIVE")
                .codigo(codigo)
                .tipo(tipo)
                .language("es")
                .bodyTemplate("Hola {{nombre}}")
                .variables(List.of("nombre"))
                .version(1)
                .active(true)
                .audiencia(Audience.builder()
                        .modo(AudienceMode.USUARIO)
                        .userIds(List.of("3", "17"))
                        .build())
                .createdAt("2026-01-01T00:00:00Z")
                .createdBy("admin");
    }

    private void assertCommonPreserved(NotificationTemplate in, NotificationTemplate out) {
        assertThat(out.getId()).isEqualTo(in.getId());
        assertThat(out.getSk()).isEqualTo(in.getSk());
        assertThat(out.getCodigo()).isEqualTo(in.getCodigo());
        assertThat(out.getTipo()).isEqualTo(in.getTipo());
        assertThat(out.getLanguage()).isEqualTo(in.getLanguage());
        assertThat(out.getBodyTemplate()).isEqualTo(in.getBodyTemplate());
        assertThat(out.getVariables()).isEqualTo(in.getVariables());
        assertThat(out.getVersion()).isEqualTo(in.getVersion());
        assertThat(out.isActive()).isEqualTo(in.isActive());
        assertThat(out.getCreatedAt()).isEqualTo(in.getCreatedAt());
        assertThat(out.getCreatedBy()).isEqualTo(in.getCreatedBy());
        assertThat(out.getAudiencia()).isNotNull();
        assertThat(out.getAudiencia().getModo()).isEqualTo(AudienceMode.USUARIO);
        assertThat(out.getAudiencia().getUserIds()).containsExactly("3", "17");
    }

    private NotificationTemplate roundTrip(NotificationTemplate in) {
        Map<String, AttributeValue> item = mapper.toItem(in);
        return mapper.toDomain(item);
    }

    @Test
    @DisplayName("SMS round-trip preserva campos comunes")
    void sms_roundTrip() {
        NotificationTemplate in = base(TemplateType.SMS, "VERIFICACION_OTP")
                .bodyTemplate("FINDU: Tu código es {{otp_code}}")
                .variables(List.of("otp_code"))
                .build();
        NotificationTemplate out = roundTrip(in);
        assertThat(out.getTipo()).isEqualTo(TemplateType.SMS);
        assertThat(out.getBodyTemplate()).isEqualTo("FINDU: Tu código es {{otp_code}}");
        assertThat(out.getVariables()).containsExactly("otp_code");
    }

    @Test
    @DisplayName("EMAIL round-trip preserva subject y contentType")
    void email_roundTrip() {
        NotificationTemplate in = base(TemplateType.EMAIL, "OFERTA_RECIBIDA")
                .subject("Nueva oferta para {{servicio}}")
                .contentType("HTML")
                .build();
        NotificationTemplate out = roundTrip(in);
        assertCommonPreserved(in, out);
        assertThat(out.getSubject()).isEqualTo("Nueva oferta para {{servicio}}");
        assertThat(out.getContentType()).isEqualTo("HTML");
    }

    @Test
    @DisplayName("WHATSAPP round-trip preserva waTemplateName y contentType")
    void whatsapp_roundTrip() {
        NotificationTemplate in = base(TemplateType.WHATSAPP, "OFERTA_RECIBIDA")
                .waTemplateName("oferta_recibida_v1")
                .contentType("TXT")
                .build();
        NotificationTemplate out = roundTrip(in);
        assertCommonPreserved(in, out);
        assertThat(out.getWaTemplateName()).isEqualTo("oferta_recibida_v1");
        assertThat(out.getContentType()).isEqualTo("TXT");
    }

    @Test
    @DisplayName("EVENTO round-trip preserva atributos de RabbitMQ")
    void evento_roundTrip() {
        NotificationTemplate in = base(TemplateType.EVENTO, "PAGO_COMPLETADO")
                .rabbitExchange("findu.notifications")
                .rabbitRoutingKey("findu.notifications.push")
                .rabbitQueue("findu.notifications.push")
                .eventType("PAYMENT_COMPLETED")
                .build();
        NotificationTemplate out = roundTrip(in);
        assertCommonPreserved(in, out);
        assertThat(out.getRabbitExchange()).isEqualTo("findu.notifications");
        assertThat(out.getRabbitRoutingKey()).isEqualTo("findu.notifications.push");
        assertThat(out.getRabbitQueue()).isEqualTo("findu.notifications.push");
        assertThat(out.getEventType()).isEqualTo("PAYMENT_COMPLETED");
    }

    @Test
    @DisplayName("NOTIFICACION round-trip preserva title y routing")
    void notificacion_roundTrip() {
        NotificationTemplate in = base(TemplateType.NOTIFICACION, "OFERTA_ACEPTADA")
                .title("Oferta aceptada")
                .routing(NotificationRouting.builder()
                        .condicion("isUserActivo")
                        .canalSiActivo("WEBSOCKET")
                        .canalSiInactivo("PUSH")
                        .build())
                .build();
        NotificationTemplate out = roundTrip(in);
        assertCommonPreserved(in, out);
        assertThat(out.getTitle()).isEqualTo("Oferta aceptada");
        assertThat(out.getRouting()).isNotNull();
        assertThat(out.getRouting().getCondicion()).isEqualTo("isUserActivo");
        assertThat(out.getRouting().getCanalSiActivo()).isEqualTo("WEBSOCKET");
        assertThat(out.getRouting().getCanalSiInactivo()).isEqualTo("PUSH");
    }

    @Test
    @DisplayName("ALERTA round-trip preserva AlertMeta")
    void alerta_roundTrip() {
        NotificationTemplate in = base(TemplateType.ALERTA, "VERIFICA_CORREO")
                .alertMeta(AlertMeta.builder()
                        .categoria(AlertCategory.SEGURIDAD)
                        .pantallaDestino("perfil_proveedor")
                        .titulo("Verifica tu correo")
                        .ctaLabel("Verificar ahora")
                        .ctaAction("findu://perfil/verificacion")
                        .cerrable(false)
                        .prioridad(1)
                        .condicionAparicion("emailVerified == false")
                        .tiempoDesaparicion(0)
                        .build())
                .build();
        NotificationTemplate out = roundTrip(in);
        assertCommonPreserved(in, out);
        AlertMeta meta = out.getAlertMeta();
        assertThat(meta).isNotNull();
        assertThat(meta.getCategoria()).isEqualTo(AlertCategory.SEGURIDAD);
        assertThat(meta.getPantallaDestino()).isEqualTo("perfil_proveedor");
        assertThat(meta.getTitulo()).isEqualTo("Verifica tu correo");
        assertThat(meta.getCtaLabel()).isEqualTo("Verificar ahora");
        assertThat(meta.getCtaAction()).isEqualTo("findu://perfil/verificacion");
        assertThat(meta.isCerrable()).isFalse();
        assertThat(meta.getPrioridad()).isEqualTo(1);
        assertThat(meta.getCondicionAparicion()).isEqualTo("emailVerified == false");
        assertThat(meta.getTiempoDesaparicion()).isEqualTo(0);
    }
}
