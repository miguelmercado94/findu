package com.findu.notification.processor.application.service.impl;

import com.findu.notification.processor.application.dto.response.ScreenAlertResponse;
import com.findu.notification.processor.application.port.in.ResolveScreenAlertsUseCase;
import com.findu.notification.processor.application.port.in.ResolveTemplateUseCase;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.AlertMeta;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resuelve las alertas de UI de una pantalla por código (diseño §7.4). El micro conoce los
 * códigos de alerta de cada pantalla (mapa en-config pequeño) y los resuelve con el hot path
 * {@link ResolveTemplateUseCase#resolveActive} — un {@code GetItem} sobre {@code v#ACTIVE}
 * por código — sin requerir el GSI por {@code pantalla_destino}.
 */
@Service
public class ScreenAlertServiceImpl implements ResolveScreenAlertsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScreenAlertServiceImpl.class);
    private static final String DEFAULT_LANGUAGE = "es";

    /**
     * Mapa pantalla -> códigos de alerta conocidos. Mantenerlo aquí permite agregar más
     * pantallas sin provisionar un GSI por {@code pantalla_destino}.
     */
    private static final Map<String, List<String>> SCREEN_ALERT_CODES = Map.of(
            "perfil_proveedor", List.of("PERFIL_CORREO_PENDIENTE", "PERFIL_BIOMETRIA_PENDIENTE")
    );

    private final ResolveTemplateUseCase resolveTemplateUseCase;

    public ScreenAlertServiceImpl(ResolveTemplateUseCase resolveTemplateUseCase) {
        this.resolveTemplateUseCase = resolveTemplateUseCase;
    }

    @Override
    public List<ScreenAlertResponse> resolveForScreen(String pantalla, String language) {
        String lang = (language == null || language.isBlank()) ? DEFAULT_LANGUAGE : language;
        List<String> codes = SCREEN_ALERT_CODES.getOrDefault(pantalla, List.of());
        if (codes.isEmpty()) {
            log.debug("No hay códigos de alerta mapeados para la pantalla '{}'", pantalla);
            return List.of();
        }

        return codes.stream()
                .map(code -> resolveTemplateUseCase.resolveActive(code, TemplateType.ALERTA, lang))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toResponse)
                .sorted(Comparator.comparing(ScreenAlertResponse::getPrioridad,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private ScreenAlertResponse toResponse(NotificationTemplate tpl) {
        AlertMeta meta = tpl.getAlertMeta();
        ScreenAlertResponse.ScreenAlertResponseBuilder b = ScreenAlertResponse.builder()
                .codigo(tpl.getCodigo())
                .body(tpl.getBodyTemplate());
        if (meta != null) {
            b.categoria(meta.getCategoria())
                    .titulo(meta.getTitulo())
                    .ctaLabel(meta.getCtaLabel())
                    .ctaAction(meta.getCtaAction())
                    .prioridad(meta.getPrioridad())
                    .condicionAparicion(meta.getCondicionAparicion())
                    .tiempoDesaparicion(meta.getTiempoDesaparicion());
        }
        return b.build();
    }
}
