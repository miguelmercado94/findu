package com.findu.notification.processor.infrastructure.adapter.out.persistence.mapper;

import com.findu.notification.processor.domain.enums.AlertCategory;
import com.findu.notification.processor.domain.enums.AudienceMode;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.AlertMeta;
import com.findu.notification.processor.domain.model.Audience;
import com.findu.notification.processor.domain.model.NotificationRouting;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import com.findu.notification.processor.infrastructure.adapter.out.persistence.entity.NotificationTemplateItem;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Único punto que conoce los nombres de atributo del ítem DynamoDB. Convierte
 * entre el modelo de dominio {@link NotificationTemplate} y el mapa de
 * {@link AttributeValue} de DynamoDB (diseño §3). Mapea los value objects
 * {@link Audience}, {@link AlertMeta} y {@link NotificationRouting} a/desde M y L&lt;S&gt;.
 */
@Component
public class NotificationTemplateItemMapper {

    // ====================== Item (DynamoDB) -> Domain ======================

    public NotificationTemplate toDomain(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        NotificationTemplate.NotificationTemplateBuilder b = NotificationTemplate.builder()
                .id(str(item, NotificationTemplateItem.ID))
                .sk(str(item, NotificationTemplateItem.SK))
                .codigo(str(item, NotificationTemplateItem.CODIGO))
                .tipo(tipo(item))
                .language(str(item, NotificationTemplateItem.LANGUAGE))
                .bodyTemplate(str(item, NotificationTemplateItem.BODY_TEMPLATE))
                .variables(stringList(item, NotificationTemplateItem.VARIABLES))
                .version(num(item, NotificationTemplateItem.VERSION))
                .active(bool(item, NotificationTemplateItem.IS_ACTIVE, true))
                .audiencia(toAudience(item.get(NotificationTemplateItem.AUDIENCIA)))
                .createdAt(str(item, NotificationTemplateItem.CREATED_AT))
                .createdBy(str(item, NotificationTemplateItem.CREATED_BY))
                .updatedAt(str(item, NotificationTemplateItem.UPDATED_AT))
                .updatedBy(str(item, NotificationTemplateItem.UPDATED_BY))
                // legacy / canal
                .channel(str(item, NotificationTemplateItem.CHANNEL))
                // EMAIL / WHATSAPP
                .subject(str(item, NotificationTemplateItem.SUBJECT))
                .contentType(str(item, NotificationTemplateItem.CONTENT_TYPE))
                .waTemplateName(str(item, NotificationTemplateItem.WA_TEMPLATE_NAME))
                // EVENTO
                .rabbitExchange(str(item, NotificationTemplateItem.RABBIT_EXCHANGE))
                .rabbitRoutingKey(str(item, NotificationTemplateItem.RABBIT_ROUTING_KEY))
                .rabbitQueue(str(item, NotificationTemplateItem.RABBIT_QUEUE))
                .eventType(str(item, NotificationTemplateItem.EVENT_TYPE))
                // NOTIFICACION
                .title(str(item, NotificationTemplateItem.TITLE))
                .routing(toRouting(item.get(NotificationTemplateItem.ROUTING)))
                // ALERTA
                .alertMeta(toAlertMeta(item.get(NotificationTemplateItem.ALERT_META)));

        return b.build();
    }

    private TemplateType tipo(Map<String, AttributeValue> item) {
        String t = str(item, NotificationTemplateItem.TIPO);
        return t != null ? TemplateType.valueOf(t.toUpperCase()) : null;
    }

    private Audience toAudience(AttributeValue av) {
        if (av == null || !av.hasM()) {
            return null;
        }
        Map<String, AttributeValue> m = av.m();
        AudienceMode modo = null;
        if (m.containsKey("modo") && m.get("modo").s() != null) {
            modo = AudienceMode.valueOf(m.get("modo").s().toUpperCase());
        }
        return Audience.builder()
                .modo(modo)
                .filtros(toPlainMap(m.get("filtros")))
                .userIds(stringListFrom(m.get("userIds")))
                .build();
    }

    private NotificationRouting toRouting(AttributeValue av) {
        if (av == null || !av.hasM()) {
            return null;
        }
        Map<String, AttributeValue> m = av.m();
        return NotificationRouting.builder()
                .condicion(strFrom(m.get(NotificationTemplateItem.ROUTING_CONDICION)))
                .canalSiActivo(strFrom(m.get(NotificationTemplateItem.ROUTING_CANAL_ACTIVO)))
                .canalSiInactivo(strFrom(m.get(NotificationTemplateItem.ROUTING_CANAL_INACTIVO)))
                .build();
    }

    private AlertMeta toAlertMeta(AttributeValue av) {
        if (av == null || !av.hasM()) {
            return null;
        }
        Map<String, AttributeValue> m = av.m();
        AlertCategory categoria = null;
        AttributeValue cat = m.get(NotificationTemplateItem.CATEGORIA);
        if (cat != null && cat.s() != null) {
            categoria = AlertCategory.valueOf(cat.s().toUpperCase());
        }
        return AlertMeta.builder()
                .categoria(categoria)
                .pantallaDestino(strFrom(m.get(NotificationTemplateItem.PANTALLA_DESTINO)))
                .titulo(strFrom(m.get(NotificationTemplateItem.TITULO)))
                .ctaLabel(strFrom(m.get(NotificationTemplateItem.CTA_LABEL)))
                .ctaAction(strFrom(m.get(NotificationTemplateItem.CTA_ACTION)))
                .cerrable(boolFrom(m.get(NotificationTemplateItem.CERRABLE)))
                .prioridad(numFrom(m.get(NotificationTemplateItem.PRIORIDAD)))
                .vigenciaDesde(strFrom(m.get(NotificationTemplateItem.VIGENCIA_DESDE)))
                .vigenciaHasta(strFrom(m.get(NotificationTemplateItem.VIGENCIA_HASTA)))
                .condicionAparicion(strFrom(m.get(NotificationTemplateItem.CONDICION_APARICION)))
                .tiempoDesaparicion(numFrom(m.get(NotificationTemplateItem.TIEMPO_DESAPARICION)))
                .build();
    }

    // ====================== Domain -> Item (DynamoDB) ======================

    public Map<String, AttributeValue> toItem(NotificationTemplate tpl) {
        Map<String, AttributeValue> item = new HashMap<>();

        putS(item, NotificationTemplateItem.ID, tpl.getId());
        putS(item, NotificationTemplateItem.SK, tpl.getSk());
        putS(item, NotificationTemplateItem.CODIGO, tpl.getCodigo());
        if (tpl.getTipo() != null) {
            putS(item, NotificationTemplateItem.TIPO, tpl.getTipo().name());
        }
        putS(item, NotificationTemplateItem.LANGUAGE, tpl.getLanguage());
        putS(item, NotificationTemplateItem.BODY_TEMPLATE, tpl.getBodyTemplate());
        putStringList(item, NotificationTemplateItem.VARIABLES, tpl.getVariables());
        if (tpl.getVersion() != null) {
            item.put(NotificationTemplateItem.VERSION, AttributeValue.fromN(String.valueOf(tpl.getVersion())));
        }
        item.put(NotificationTemplateItem.IS_ACTIVE, AttributeValue.fromBool(tpl.isActive()));
        putAudience(item, tpl.getAudiencia());
        putS(item, NotificationTemplateItem.CREATED_AT, tpl.getCreatedAt());
        putS(item, NotificationTemplateItem.CREATED_BY, tpl.getCreatedBy());
        putS(item, NotificationTemplateItem.UPDATED_AT, tpl.getUpdatedAt());
        putS(item, NotificationTemplateItem.UPDATED_BY, tpl.getUpdatedBy());

        putS(item, NotificationTemplateItem.CHANNEL, tpl.getChannel());

        putS(item, NotificationTemplateItem.SUBJECT, tpl.getSubject());
        putS(item, NotificationTemplateItem.CONTENT_TYPE, tpl.getContentType());
        putS(item, NotificationTemplateItem.WA_TEMPLATE_NAME, tpl.getWaTemplateName());

        putS(item, NotificationTemplateItem.RABBIT_EXCHANGE, tpl.getRabbitExchange());
        putS(item, NotificationTemplateItem.RABBIT_ROUTING_KEY, tpl.getRabbitRoutingKey());
        putS(item, NotificationTemplateItem.RABBIT_QUEUE, tpl.getRabbitQueue());
        putS(item, NotificationTemplateItem.EVENT_TYPE, tpl.getEventType());

        putS(item, NotificationTemplateItem.TITLE, tpl.getTitle());
        putRouting(item, tpl.getRouting());

        putAlertMeta(item, tpl.getAlertMeta());

        return item;
    }

    private void putAudience(Map<String, AttributeValue> item, Audience audiencia) {
        if (audiencia == null) {
            return;
        }
        Map<String, AttributeValue> m = new HashMap<>();
        if (audiencia.getModo() != null) {
            m.put("modo", AttributeValue.fromS(audiencia.getModo().name()));
        }
        if (audiencia.getFiltros() != null && !audiencia.getFiltros().isEmpty()) {
            m.put("filtros", AttributeValue.fromM(toAttributeMap(audiencia.getFiltros())));
        }
        if (audiencia.getUserIds() != null) {
            m.put("userIds", stringListAttr(audiencia.getUserIds()));
        }
        if (!m.isEmpty()) {
            item.put(NotificationTemplateItem.AUDIENCIA, AttributeValue.fromM(m));
        }
    }

    private void putRouting(Map<String, AttributeValue> item, NotificationRouting routing) {
        if (routing == null) {
            return;
        }
        Map<String, AttributeValue> m = new HashMap<>();
        putS(m, NotificationTemplateItem.ROUTING_CONDICION, routing.getCondicion());
        putS(m, NotificationTemplateItem.ROUTING_CANAL_ACTIVO, routing.getCanalSiActivo());
        putS(m, NotificationTemplateItem.ROUTING_CANAL_INACTIVO, routing.getCanalSiInactivo());
        if (!m.isEmpty()) {
            item.put(NotificationTemplateItem.ROUTING, AttributeValue.fromM(m));
        }
    }

    private void putAlertMeta(Map<String, AttributeValue> item, AlertMeta alertMeta) {
        if (alertMeta == null) {
            return;
        }
        Map<String, AttributeValue> m = new HashMap<>();
        if (alertMeta.getCategoria() != null) {
            m.put(NotificationTemplateItem.CATEGORIA, AttributeValue.fromS(alertMeta.getCategoria().name()));
        }
        putS(m, NotificationTemplateItem.PANTALLA_DESTINO, alertMeta.getPantallaDestino());
        putS(m, NotificationTemplateItem.TITULO, alertMeta.getTitulo());
        putS(m, NotificationTemplateItem.CTA_LABEL, alertMeta.getCtaLabel());
        putS(m, NotificationTemplateItem.CTA_ACTION, alertMeta.getCtaAction());
        m.put(NotificationTemplateItem.CERRABLE, AttributeValue.fromBool(alertMeta.isCerrable()));
        if (alertMeta.getPrioridad() != null) {
            m.put(NotificationTemplateItem.PRIORIDAD, AttributeValue.fromN(String.valueOf(alertMeta.getPrioridad())));
        }
        putS(m, NotificationTemplateItem.VIGENCIA_DESDE, alertMeta.getVigenciaDesde());
        putS(m, NotificationTemplateItem.VIGENCIA_HASTA, alertMeta.getVigenciaHasta());
        putS(m, NotificationTemplateItem.CONDICION_APARICION, alertMeta.getCondicionAparicion());
        if (alertMeta.getTiempoDesaparicion() != null) {
            m.put(NotificationTemplateItem.TIEMPO_DESAPARICION,
                    AttributeValue.fromN(String.valueOf(alertMeta.getTiempoDesaparicion())));
        }
        item.put(NotificationTemplateItem.ALERT_META, AttributeValue.fromM(m));
    }

    // ====================== Helpers de lectura ======================

    private String str(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : null;
    }

    private String strFrom(AttributeValue av) {
        return av != null ? av.s() : null;
    }

    private Integer num(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) && item.get(key).n() != null
                ? Integer.valueOf(item.get(key).n()) : null;
    }

    private Integer numFrom(AttributeValue av) {
        return av != null && av.n() != null ? Integer.valueOf(av.n()) : null;
    }

    private boolean bool(Map<String, AttributeValue> item, String key, boolean defaultValue) {
        if (!item.containsKey(key) || item.get(key).bool() == null) {
            return defaultValue;
        }
        return item.get(key).bool();
    }

    private boolean boolFrom(AttributeValue av) {
        return av != null && av.bool() != null && av.bool();
    }

    private List<String> stringList(Map<String, AttributeValue> item, String key) {
        return stringListFrom(item.get(key));
    }

    private List<String> stringListFrom(AttributeValue av) {
        if (av == null || !av.hasL()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (AttributeValue element : av.l()) {
            values.add(element.s());
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toPlainMap(AttributeValue av) {
        if (av == null || !av.hasM()) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, AttributeValue> e : av.m().entrySet()) {
            result.put(e.getKey(), toPlainValue(e.getValue()));
        }
        return result;
    }

    private Object toPlainValue(AttributeValue av) {
        if (av.s() != null) {
            return av.s();
        }
        if (av.n() != null) {
            return av.n();
        }
        if (av.bool() != null) {
            return av.bool();
        }
        if (av.hasL()) {
            List<Object> list = new ArrayList<>();
            for (AttributeValue e : av.l()) {
                list.add(toPlainValue(e));
            }
            return list;
        }
        if (av.hasM()) {
            return toPlainMap(av);
        }
        return null;
    }

    // ====================== Helpers de escritura ======================

    private void putS(Map<String, AttributeValue> item, String key, String value) {
        if (value != null) {
            item.put(key, AttributeValue.fromS(value));
        }
    }

    private void putStringList(Map<String, AttributeValue> item, String key, List<String> values) {
        if (values != null) {
            item.put(key, stringListAttr(values));
        }
    }

    private AttributeValue stringListAttr(List<String> values) {
        List<AttributeValue> list = new ArrayList<>();
        for (String v : values) {
            list.add(AttributeValue.fromS(v));
        }
        return AttributeValue.fromL(list);
    }

    private Map<String, AttributeValue> toAttributeMap(Map<String, Object> source) {
        Map<String, AttributeValue> result = new HashMap<>();
        for (Map.Entry<String, Object> e : source.entrySet()) {
            AttributeValue av = toAttributeValue(e.getValue());
            if (av != null) {
                result.put(e.getKey(), av);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private AttributeValue toAttributeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return AttributeValue.fromS(s);
        }
        if (value instanceof Boolean b) {
            return AttributeValue.fromBool(b);
        }
        if (value instanceof Number n) {
            return AttributeValue.fromN(n.toString());
        }
        if (value instanceof List<?> list) {
            List<AttributeValue> l = new ArrayList<>();
            for (Object element : list) {
                AttributeValue av = toAttributeValue(element);
                if (av != null) {
                    l.add(av);
                }
            }
            return AttributeValue.fromL(l);
        }
        if (value instanceof Map<?, ?> map) {
            return AttributeValue.fromM(toAttributeMap((Map<String, Object>) map));
        }
        return AttributeValue.fromS(value.toString());
    }
}
