package com.findu.security.application.port.output.externalapi;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida hacia help-v2: crea el ticket de revisión de activación del proveedor.
 * Idempotente por {@code externalRef=ACT-{userId}} (dedup en help-v2).
 */
public interface SupportTicketPort {

    /**
     * Crea (o reutiliza por externalRef) el ticket de activación.
     *
     * @param userId      id del proveedor.
     * @param subject     asunto fijo del ticket.
     * @param description descripción del ticket.
     * @param externalRef clave idempotente (p. ej. ACT-{userId}).
     * @return código del ticket creado/existente.
     */
    Mono<String> createActivationTicket(Long userId, String subject, String description, String externalRef);
}
