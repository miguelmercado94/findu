package com.findu.transaction.infrastructure.adapter.out.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class CoreServiceClient {

    private final RestClient restClient;

    public CoreServiceClient(
            @Value("${findu.core.url:http://findu-core:8082/findu-core}") String coreUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(coreUrl)
                .build();
    }

    public Optional<SolicitudCoreDto> getSolicitud(Long solicitudId) {
        if (solicitudId == null) return Optional.empty();
        try {
            SolicitudCoreDto dto = restClient.get()
                    .uri("/api/v1/solicitudes/{id}", solicitudId)
                    .retrieve()
                    .body(SolicitudCoreDto.class);

            return Optional.ofNullable(dto);
        } catch (Exception e) {
            log.debug("No se pudo obtener solicitud {} de findu-core: {}", solicitudId, e.getMessage());
            return Optional.empty();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SolicitudCoreDto {
        private Long id;
        private String servicioNombre;
        private String categoriaNombre;
        private String direccionTexto;
        private String municipioNombre;
        private LocalDateTime fechaProgramada;
        private String detalles;
        private String estadoSolicitud;
    }
}
