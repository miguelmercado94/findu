package com.findu.notification.processor.application.util;

import com.findu.notification.processor.application.dto.request.AlertMetaRequest;
import com.findu.notification.processor.application.dto.request.AudienceRequest;
import com.findu.notification.processor.application.dto.request.CreateTemplateRequest;
import com.findu.notification.processor.application.dto.request.NotificationRoutingRequest;
import com.findu.notification.processor.application.dto.request.UpdateTemplateRequest;
import com.findu.notification.processor.application.dto.response.AlertMetaResponse;
import com.findu.notification.processor.application.dto.response.AudienceResponse;
import com.findu.notification.processor.application.dto.response.NotificationRoutingResponse;
import com.findu.notification.processor.application.dto.response.TemplateResponse;
import com.findu.notification.processor.application.dto.response.TemplateVersionResponse;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.AlertMeta;
import com.findu.notification.processor.domain.model.Audience;
import com.findu.notification.processor.domain.model.NotificationRouting;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.springframework.stereotype.Component;

/**
 * Mapea entre los DTOs de la superficie admin y el modelo de dominio
 * {@link NotificationTemplate} (diseño §10, application/util). Mantiene la capa de
 * servicio libre de conversiones repetitivas.
 */
@Component
public class TemplateDtoMapper {

    // ====================== Request -> Domain ======================

    public NotificationTemplate toDomain(CreateTemplateRequest req) {
        return NotificationTemplate.builder()
                .codigo(req.getCodigo())
                .tipo(req.getTipo())
                .language(req.getLanguage())
                .bodyTemplate(req.getBodyTemplate())
                .variables(req.getVariables())
                .audiencia(toDomain(req.getAudiencia()))
                .subject(req.getSubject())
                .contentType(req.getContentType())
                .waTemplateName(req.getWaTemplateName())
                .rabbitExchange(req.getRabbitExchange())
                .rabbitRoutingKey(req.getRabbitRoutingKey())
                .rabbitQueue(req.getRabbitQueue())
                .eventType(req.getEventType())
                .title(req.getTitle())
                .routing(toDomain(req.getRouting()))
                .alertMeta(toDomain(req.getAlertMeta()))
                .build();
    }

    public NotificationTemplate toDomain(String codigo, TemplateType tipo, String language, UpdateTemplateRequest req) {
        return NotificationTemplate.builder()
                .codigo(codigo)
                .tipo(tipo)
                .language(language)
                .bodyTemplate(req.getBodyTemplate())
                .variables(req.getVariables())
                .audiencia(toDomain(req.getAudiencia()))
                .subject(req.getSubject())
                .contentType(req.getContentType())
                .waTemplateName(req.getWaTemplateName())
                .rabbitExchange(req.getRabbitExchange())
                .rabbitRoutingKey(req.getRabbitRoutingKey())
                .rabbitQueue(req.getRabbitQueue())
                .eventType(req.getEventType())
                .title(req.getTitle())
                .routing(toDomain(req.getRouting()))
                .alertMeta(toDomain(req.getAlertMeta()))
                .build();
    }

    private Audience toDomain(AudienceRequest req) {
        if (req == null) {
            return null;
        }
        return Audience.builder()
                .modo(req.getModo())
                .filtros(req.getFiltros())
                .userIds(req.getUserIds())
                .build();
    }

    private NotificationRouting toDomain(NotificationRoutingRequest req) {
        if (req == null) {
            return null;
        }
        return NotificationRouting.builder()
                .condicion(req.getCondicion())
                .canalSiActivo(req.getCanalSiActivo())
                .canalSiInactivo(req.getCanalSiInactivo())
                .build();
    }

    private AlertMeta toDomain(AlertMetaRequest req) {
        if (req == null) {
            return null;
        }
        return AlertMeta.builder()
                .categoria(req.getCategoria())
                .pantallaDestino(req.getPantallaDestino())
                .titulo(req.getTitulo())
                .ctaLabel(req.getCtaLabel())
                .ctaAction(req.getCtaAction())
                .cerrable(req.isCerrable())
                .prioridad(req.getPrioridad())
                .vigenciaDesde(req.getVigenciaDesde())
                .vigenciaHasta(req.getVigenciaHasta())
                .condicionAparicion(req.getCondicionAparicion())
                .tiempoDesaparicion(req.getTiempoDesaparicion())
                .build();
    }

    // ====================== Domain -> Response ======================

    public TemplateResponse toResponse(NotificationTemplate tpl) {
        if (tpl == null) {
            return null;
        }
        return TemplateResponse.builder()
                .id(tpl.getId())
                .codigo(tpl.getCodigo())
                .tipo(tpl.getTipo())
                .language(tpl.getLanguage())
                .bodyTemplate(tpl.getBodyTemplate())
                .variables(tpl.getVariables())
                .version(tpl.getVersion())
                .active(tpl.isActive())
                .audiencia(toResponse(tpl.getAudiencia()))
                .createdAt(tpl.getCreatedAt())
                .createdBy(tpl.getCreatedBy())
                .updatedAt(tpl.getUpdatedAt())
                .updatedBy(tpl.getUpdatedBy())
                .subject(tpl.getSubject())
                .contentType(tpl.getContentType())
                .waTemplateName(tpl.getWaTemplateName())
                .rabbitExchange(tpl.getRabbitExchange())
                .rabbitRoutingKey(tpl.getRabbitRoutingKey())
                .rabbitQueue(tpl.getRabbitQueue())
                .eventType(tpl.getEventType())
                .title(tpl.getTitle())
                .routing(toResponse(tpl.getRouting()))
                .alertMeta(toResponse(tpl.getAlertMeta()))
                .build();
    }

    public TemplateVersionResponse toVersionResponse(NotificationTemplate tpl) {
        if (tpl == null) {
            return null;
        }
        return TemplateVersionResponse.builder()
                .version(tpl.getVersion())
                .active(tpl.isActive())
                .createdAt(tpl.getCreatedAt())
                .createdBy(tpl.getCreatedBy())
                .bodyTemplate(tpl.getBodyTemplate())
                .build();
    }

    private AudienceResponse toResponse(Audience audience) {
        if (audience == null) {
            return null;
        }
        return AudienceResponse.builder()
                .modo(audience.getModo())
                .filtros(audience.getFiltros())
                .userIds(audience.getUserIds())
                .build();
    }

    private NotificationRoutingResponse toResponse(NotificationRouting routing) {
        if (routing == null) {
            return null;
        }
        return NotificationRoutingResponse.builder()
                .condicion(routing.getCondicion())
                .canalSiActivo(routing.getCanalSiActivo())
                .canalSiInactivo(routing.getCanalSiInactivo())
                .build();
    }

    private AlertMetaResponse toResponse(AlertMeta meta) {
        if (meta == null) {
            return null;
        }
        return AlertMetaResponse.builder()
                .categoria(meta.getCategoria())
                .pantallaDestino(meta.getPantallaDestino())
                .titulo(meta.getTitulo())
                .ctaLabel(meta.getCtaLabel())
                .ctaAction(meta.getCtaAction())
                .cerrable(meta.isCerrable())
                .prioridad(meta.getPrioridad())
                .vigenciaDesde(meta.getVigenciaDesde())
                .vigenciaHasta(meta.getVigenciaHasta())
                .condicionAparicion(meta.getCondicionAparicion())
                .tiempoDesaparicion(meta.getTiempoDesaparicion())
                .build();
    }
}
