package com.findu.security.infrastructure.adapter.externalapi;

import com.findu.security.application.port.output.externalapi.SupportTicketPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter WebClient hacia help-v2: crea el ticket de revisión de activación.
 * Idempotente por {@code externalRef} (dedup del lado de help-v2). Propaga el error (5xx/timeout)
 * para que el use case deje {@code soporte_notified=false} y el scheduler reintente.
 */
@Component
public class SupportTicketWebClientAdapter implements SupportTicketPort {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketWebClientAdapter.class);
    private static final String USER_ROLE_PROVEEDOR = "PROVEEDOR";

    private final WebClient webClient;
    private final String ticketsPath;

    public SupportTicketWebClientAdapter(
            @Value("${findu.activation.help.base-url:http://findu-api-gateway:8080}") String baseUrl,
            @Value("${findu.activation.help.tickets-path:/findu-help-v2/api/v1/help/tickets}") String ticketsPath) {
        this.ticketsPath = ticketsPath;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /** Vista parcial de la respuesta del ticket (solo ticketCode). */
    private record TicketResponse(String ticketCode) {}

    @Override
    public Mono<String> createActivationTicket(Long userId, String subject, String description, String externalRef) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("userRole", USER_ROLE_PROVEEDOR);
        body.put("subject", subject);
        body.put("description", description);
        body.put("externalRef", externalRef);
        return webClient.post()
                .uri(ticketsPath)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TicketResponse.class)
                .map(TicketResponse::ticketCode)
                .doOnSuccess(code -> log.info("Ticket de activación creado (userId={}, ticketCode={})", userId, code))
                .doOnError(e -> log.error("Creación de ticket de activación falló (userId={}): {}", userId, e.getMessage()));
    }
}
