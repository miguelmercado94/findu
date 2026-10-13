package com.findu.notification.processor.application.port.in;

import com.findu.notification.processor.application.dto.response.ScreenAlertResponse;

import java.util.List;

/**
 * Caso de uso de lectura de alertas por pantalla (diseño §7.4).
 *
 * <p>Resuelve las plantillas tipo {@code ALERTA} activas asociadas a una pantalla
 * conocida mediante lecturas {@code findActive(codigo, ALERTA, language)} (GetItem sobre
 * {@code v#ACTIVE}, sin GSI nuevo). Devuelve solo los campos de UI que el cliente necesita,
 * ordenados por {@code prioridad}. El cliente evalúa {@code condicionAparicion} contra el
 * estado del perfil para decidir qué alertas renderizar.</p>
 */
public interface ResolveScreenAlertsUseCase {

    /**
     * Devuelve las alertas activas para la pantalla dada (vacío si no hay códigos
     * mapeados o ninguna versión activa). Aplica fallback de idioma a {@code es}.
     */
    List<ScreenAlertResponse> resolveForScreen(String pantalla, String language);
}
