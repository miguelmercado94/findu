package com.findu.transaction.infrastructure.adapter.out.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class ValidationBrebClient {

    private final RestClient restClient;

    public ValidationBrebClient(
            @Value("${findu.validacion.url:http://findu-validacion:8088/findu-validacion}") String validacionUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(validacionUrl)
                .build();
    }

    public Optional<ProviderBrebKeyDto> getActiveBrebKey(Long providerId) {
        if (providerId == null) return Optional.empty();
        try {
            ProviderBrebKeyDto dto = restClient.get()
                    .uri("/api/v1/validations/bre-b-keys/{providerId}", providerId)
                    .retrieve()
                    .body(ProviderBrebKeyDto.class);

            if (dto != null && Boolean.TRUE.equals(dto.getActive()) && dto.getKeyValue() != null) {
                log.info("Llave Bre-B activa recuperada de findu-validacion para providerId={}: keyType={}, bank={}",
                        providerId, dto.getKeyType(), dto.getBankName());
                return Optional.of(dto);
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener llave Bre-B de findu-validacion para providerId={}: {}", providerId, e.getMessage());
        }
        return Optional.empty();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderBrebKeyDto {
        private String id;
        private Long providerId;
        private String keyType;
        private String keyValue;
        private String maskedKeyValue;
        private String bankCode;
        private String bankName;
        private Boolean active;
    }
}
