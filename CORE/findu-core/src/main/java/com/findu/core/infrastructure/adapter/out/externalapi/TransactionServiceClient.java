package com.findu.core.infrastructure.adapter.out.externalapi;

import com.findu.core.domain.model.SolicitudServicio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class TransactionServiceClient {

    private final RestClient restClient;

    public TransactionServiceClient(
            @Value("${findu.transaction.url:http://findu-transaction:8086}") String transactionUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(transactionUrl)
                .build();
    }

    public void registrarCreditoServicio(SolicitudServicio solicitud, Long providerId, BigDecimal amount) {
        if (solicitud == null || providerId == null) return;
        try {
            BigDecimal serviceAmount = amount != null && amount.compareTo(BigDecimal.ZERO) > 0
                    ? amount
                    : (solicitud.getPresupuestoMaximo() != null ? solicitud.getPresupuestoMaximo() : BigDecimal.valueOf(100000));

            Map<String, Object> body = Map.of(
                    "serviceRequestId", solicitud.getId(),
                    "providerId", providerId,
                    "customerId", solicitud.getPerfilClienteId() != null ? solicitud.getPerfilClienteId() : 1L,
                    "serviceAmount", serviceAmount,
                    "commissionRate", new BigDecimal("0.10"),
                    "tipAmount", BigDecimal.ZERO,
                    "extraAmount", BigDecimal.ZERO,
                    "paymentMethod", "CARD"
            );

            restClient.post()
                    .uri("/api/v1/transactions")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Transacción financiera registrada exitosamente en findu-transaction para solicitudId={}, providerId={}, amount={}",
                    solicitud.getId(), providerId, serviceAmount);
        } catch (Exception e) {
            log.warn("No se pudo registrar transacción financiera en findu-transaction: {}", e.getMessage());
        }
    }
}
