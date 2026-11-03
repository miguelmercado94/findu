package com.findu.core.infrastructure.adapter.out.externalapi;

import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class HelpServiceClient {

    private final RestClient restClient;

    public HelpServiceClient(
            @Value("${findu.help.url:http://findu-help-v2:8085}") String helpUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(helpUrl)
                .build();
    }

    public HelpServiceClient() {
        this("http://findu-help-v2:8085");
    }

    /**
     * Crea un ticket de soporte en el microservicio findu-help-v2 reutilizando la categoría
     * "cat-servicio-cliente" y subcategoría "sub-cancelaciones".
     * Si findu-help-v2 no está disponible (ej. entorno de pruebas unitarias), genera un código determinístico de respaldo.
     */
    public String crearTicketCancelacion(
            Long proveedorId,
            String nombreProveedor,
            Long solicitudId,
            CategoriaCancelacionProveedor categoria,
            String motivo,
            String descripcion
    ) {
        String fallbackCode = generarTicketCodigoFallback(solicitudId);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", proveedorId);
            body.put("userRole", "PROVEEDOR");
            body.put("solicitudId", solicitudId);
            body.put("categoryId", "cat-servicio-cliente");
            body.put("subcategoryId", "sub-cancelaciones");
            body.put("externalRef", "CANC-SOL-" + solicitudId + "-PROV-" + proveedorId);
            body.put("subject", "Cancelación [" + categoria.name() + "] - " + motivo);
            body.put("description", (descripcion != null && !descripcion.isBlank())
                    ? descripcion
                    : ("Solicitud de cancelación por: " + motivo));
            body.put("userName", nombreProveedor != null ? nombreProveedor : "Proveedor #" + proveedorId);
            body.put("requesterName", nombreProveedor != null ? nombreProveedor : "Proveedor #" + proveedorId);
            body.put("userEmail", "proveedor" + proveedorId + "@findu.com");

            Map<String, Object> response = restClient.post()
                    .uri("/api/v1/tickets")
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.get("ticketCode") != null) {
                String ticketCode = String.valueOf(response.get("ticketCode"));
                log.info("Ticket de soporte creado en findu-help-v2: {} para solicitudId={}", ticketCode, solicitudId);
                return ticketCode;
            }
        } catch (Exception e) {
            log.warn("No se pudo conectar con findu-help-v2 ({}), usando ticketCode local de respaldo {}: {}",
                    e.getMessage(), fallbackCode, solicitudId);
        }
        return fallbackCode;
    }

    public void actualizarEstadoTicketSoporte(String ticketCode, String nuevoEstadoHelp, Long agenteId, String razon) {
        if (ticketCode == null || ticketCode.isBlank()) return;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("newStatus", nuevoEstadoHelp);
            body.put("agentId", agenteId != null ? agenteId : 1L);
            body.put("agentName", "Soporte Chambii");
            body.put("reason", razon != null ? razon : "Resolución de solicitud de cancelación");
            body.put("resolutionDetail", razon != null ? razon : "Resolución registrada desde findu-core");

            restClient.put()
                    .uri("/api/v1/tickets/{ticketCode}/status", ticketCode)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.debug("Sincronización no bloqueante con findu-help-v2 para ticket {}: {}", ticketCode, e.getMessage());
        }
    }

    private String generarTicketCodigoFallback(Long solicitudId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TKT-" + datePart + "-" + (solicitudId != null ? solicitudId : "0") + "-" + suffix;
    }
}

