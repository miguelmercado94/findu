package com.findu.transaction.refund.service;

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
public class RefundTaskService {

    @Value("${findu.transaction.url:http://localhost:8086}")
    private String transactionServiceUrl;

    public Map<String, Object> runRefunds() {
        log.info("Iniciando tarea batch de procesamiento de devoluciones/refunds pendientes...");
        RestClient client = RestClient.builder().baseUrl(transactionServiceUrl).build();

        try {
            List<?> pendingRefunds = client.get()
                    .uri("/api/v1/refunds/pending")
                    .retrieve()
                    .body(List.class);

            if (pendingRefunds == null || pendingRefunds.isEmpty()) {
                return Map.of("status", "SUCCESS", "message", "No hay refunds pendientes por procesar.", "processedCount", 0);
            }

            List<Object> executedResults = new ArrayList<>();
            for (Object item : pendingRefunds) {
                if (item instanceof Map<?, ?> refundMap) {
                    Object refundId = refundMap.get("id");
                    if (refundId != null) {
                        Object result = executeRefund(refundId.toString(), "SUCCESS");
                        executedResults.add(result);
                    }
                }
            }

            return Map.of(
                    "status", "SUCCESS",
                    "message", "Procesamiento de refunds completado.",
                    "processedCount", executedResults.size(),
                    "details", executedResults
            );
        } catch (Exception e) {
            log.error("Error al consultar o ejecutar refunds pendientes: {}", e.getMessage(), e);
            return Map.of("status", "FAILED", "error", e.getMessage());
        }
    }

    public Object executeRefund(String refundId, String desiredOutcome) {
        log.info("Ejecutando refund individual id: {} con outcome simulado: {}", refundId, desiredOutcome);
        RestClient client = RestClient.builder().baseUrl(transactionServiceUrl).build();
        String outcomeStr = desiredOutcome != null ? desiredOutcome : "SUCCESS";

        return client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/refunds/{refundId}/execute")
                        .queryParam("outcome", outcomeStr)
                        .build(refundId))
                .retrieve()
                .body(Object.class);
    }
}
