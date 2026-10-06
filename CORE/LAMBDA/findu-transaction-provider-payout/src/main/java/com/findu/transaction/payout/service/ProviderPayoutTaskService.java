package com.findu.transaction.payout.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderPayoutTaskService {

    @Value("${findu.transaction.url:http://localhost:8086}")
    private String transactionServiceUrl;

    public Map<String, Object> runPayouts() {
        log.info("Iniciando tarea batch de procesamiento de payouts pendientes...");
        RestClient client = RestClient.builder().baseUrl(transactionServiceUrl).build();

        try {
            List<?> pendingPayouts = client.get()
                    .uri("/api/v1/payouts/pending")
                    .retrieve()
                    .body(List.class);

            if (pendingPayouts == null || pendingPayouts.isEmpty()) {
                return Map.of("status", "SUCCESS", "message", "No hay payouts pendientes por procesar.", "processedCount", 0);
            }

            List<Object> executedResults = new ArrayList<>();
            for (Object item : pendingPayouts) {
                if (item instanceof Map<?, ?> payoutMap) {
                    Object payoutId = payoutMap.get("id");
                    if (payoutId != null) {
                        Object result = executePayout(payoutId.toString(), "SUCCESS");
                        executedResults.add(result);
                    }
                }
            }

            return Map.of(
                    "status", "SUCCESS",
                    "message", "Procesamiento de payouts completado.",
                    "processedCount", executedResults.size(),
                    "details", executedResults
            );
        } catch (Exception e) {
            log.error("Error al consultar o ejecutar payouts pendientes: {}", e.getMessage(), e);
            return Map.of("status", "FAILED", "error", e.getMessage());
        }
    }

    public Object executePayout(String payoutId, String desiredOutcome) {
        log.info("Ejecutando payout individual id: {} con outcome simulado: {}", payoutId, desiredOutcome);
        RestClient client = RestClient.builder().baseUrl(transactionServiceUrl).build();
        String outcomeStr = desiredOutcome != null ? desiredOutcome : "SUCCESS";

        return client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/payouts/{payoutId}/execute")
                        .queryParam("outcome", outcomeStr)
                        .build(payoutId))
                .retrieve()
                .body(Object.class);
    }
}
