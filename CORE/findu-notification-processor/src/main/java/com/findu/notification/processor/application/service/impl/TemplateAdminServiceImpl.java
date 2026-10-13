package com.findu.notification.processor.application.service.impl;

import com.findu.notification.processor.application.dto.request.ActivateVersionRequest;
import com.findu.notification.processor.application.dto.request.CreateTemplateRequest;
import com.findu.notification.processor.application.dto.request.UpdateTemplateRequest;
import com.findu.notification.processor.application.dto.response.TemplateResponse;
import com.findu.notification.processor.application.dto.response.TemplateVersionResponse;
import com.findu.notification.processor.application.port.in.ManageTemplateUseCase;
import com.findu.notification.processor.application.port.out.persistence.TemplateRepositoryPort;
import com.findu.notification.processor.application.util.TemplateDtoMapper;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.exception.InvalidTemplateException;
import com.findu.notification.processor.domain.exception.TemplateNotFoundException;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de la superficie de administración de plantillas (diseño §10).
 *
 * <p>Delega la persistencia versionada en {@link TemplateRepositoryPort} (FEAT-002):
 * {@code edit} genera una versión nueva inmutable (nunca sobreescribe), {@code activateVersion}
 * repunta el {@code v#ACTIVE} a una versión previa (rollback) y todas las mutaciones
 * invalidan la entrada de caché {@code notification-templates} del hot path de envío.</p>
 */
@Service
public class TemplateAdminServiceImpl implements ManageTemplateUseCase {

    private static final Logger log = LoggerFactory.getLogger(TemplateAdminServiceImpl.class);
    private static final String CACHE_NAME = "notification-templates";
    private static final String DEFAULT_ACTOR = "admin";

    private final TemplateRepositoryPort templateRepositoryPort;
    private final TemplateDtoMapper mapper;
    private final CacheManager cacheManager;

    public TemplateAdminServiceImpl(TemplateRepositoryPort templateRepositoryPort,
                                    TemplateDtoMapper mapper,
                                    CacheManager cacheManager) {
        this.templateRepositoryPort = templateRepositoryPort;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
    }

    @Override
    public TemplateResponse create(CreateTemplateRequest request) {
        NotificationTemplate template = mapper.toDomain(request);
        validatePerTypeFields(template);

        String now = Instant.now().toString();
        template.setCreatedAt(now);
        template.setCreatedBy(DEFAULT_ACTOR);
        template.setUpdatedAt(now);
        template.setUpdatedBy(DEFAULT_ACTOR);

        templateRepositoryPort.createNewVersion(template);
        evict(template.getCodigo(), template.getTipo(), template.getLanguage());

        return reloadActive(template.getCodigo(), template.getTipo(), template.getLanguage());
    }

    @Override
    public TemplateResponse edit(String codigo, TemplateType tipo, String language, UpdateTemplateRequest request) {
        NotificationTemplate template = mapper.toDomain(codigo, tipo, language, request);
        validatePerTypeFields(template);

        String now = Instant.now().toString();
        // Conservar la auditoría de creación de la versión original si existe.
        templateRepositoryPort.findActive(codigo, tipo, language).ifPresentOrElse(active -> {
            template.setCreatedAt(active.getCreatedAt());
            template.setCreatedBy(active.getCreatedBy());
        }, () -> {
            template.setCreatedAt(now);
            template.setCreatedBy(DEFAULT_ACTOR);
        });
        template.setUpdatedAt(now);
        template.setUpdatedBy(DEFAULT_ACTOR);

        // createNewVersion escribe v#<n+1> inmutable y repunta v#ACTIVE (nunca sobreescribe).
        templateRepositoryPort.createNewVersion(template);
        evict(codigo, tipo, language);

        return reloadActive(codigo, tipo, language);
    }

    @Override
    public TemplateResponse activateVersion(ActivateVersionRequest request) {
        if (request.getVersion() == null) {
            throw new InvalidTemplateException("version es requerida para activar una plantilla");
        }
        String id = buildId(request.getCodigo(), request.getTipo(), request.getLanguage());

        templateRepositoryPort.activateVersion(id, request.getVersion());
        evict(request.getCodigo(), request.getTipo(), request.getLanguage());

        return reloadActive(request.getCodigo(), request.getTipo(), request.getLanguage());
    }

    @Override
    public List<TemplateResponse> listByType(TemplateType tipo) {
        return templateRepositoryPort.findByTipo(tipo).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<TemplateVersionResponse> history(String codigo, TemplateType tipo, String language) {
        String id = buildId(codigo, tipo, language);
        return templateRepositoryPort.findHistory(id).stream()
                .map(mapper::toVersionResponse)
                .toList();
    }

    @Override
    public void deactivate(String codigo, TemplateType tipo, String language) {
        String id = buildId(codigo, tipo, language);
        templateRepositoryPort.deactivate(id);
        evict(codigo, tipo, language);
    }

    // ====================== Helpers ======================

    private TemplateResponse reloadActive(String codigo, TemplateType tipo, String language) {
        Optional<NotificationTemplate> active = templateRepositoryPort.findActive(codigo, tipo, language);
        return active.map(mapper::toResponse)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "No se encontró la versión activa tras la operación: " + buildId(codigo, tipo, language)));
    }

    /**
     * Valida los campos obligatorios según el tipo funcional (diseño §3.2). Los campos
     * comunes (codigo/tipo/language/bodyTemplate) ya los garantiza jakarta.validation en el DTO.
     */
    private void validatePerTypeFields(NotificationTemplate template) {
        TemplateType tipo = template.getTipo();
        if (tipo == null) {
            throw new InvalidTemplateException("tipo es requerido");
        }
        switch (tipo) {
            case EMAIL -> {
                if (isBlank(template.getSubject())) {
                    throw new InvalidTemplateException("subject es requerido para plantillas de tipo EMAIL");
                }
            }
            case WHATSAPP -> {
                if (isBlank(template.getWaTemplateName())) {
                    throw new InvalidTemplateException("waTemplateName es requerido para plantillas de tipo WHATSAPP");
                }
            }
            case EVENTO -> {
                if (isBlank(template.getRabbitExchange()) || isBlank(template.getRabbitRoutingKey())) {
                    throw new InvalidTemplateException(
                            "rabbitExchange y rabbitRoutingKey son requeridos para plantillas de tipo EVENTO");
                }
            }
            case NOTIFICACION -> {
                if (template.getRouting() == null) {
                    throw new InvalidTemplateException("routing es requerido para plantillas de tipo NOTIFICACION");
                }
            }
            case ALERTA -> {
                if (template.getAlertMeta() == null || template.getAlertMeta().getCategoria() == null) {
                    throw new InvalidTemplateException(
                            "alertMeta.categoria es requerida para plantillas de tipo ALERTA");
                }
            }
            case SMS -> {
                // Sólo requiere los campos comunes.
            }
        }
    }

    private void evict(String codigo, TemplateType tipo, String language) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return;
        }
        // Debe coincidir con la clave de @Cacheable de TemplateServiceImpl:
        // "#templateCode + '#' + #tipo + '#' + #language" (tipo -> TemplateType.toString()).
        cache.evict(codigo + "#" + tipo + "#" + language);
        log.debug("Caché de plantilla invalidada: {}#{}#{}", codigo, tipo, language);
    }

    private String buildId(String codigo, TemplateType tipo, String language) {
        return codigo.toUpperCase() + "#" + tipo.name().toUpperCase() + "#" + language.toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
