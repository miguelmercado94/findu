package com.findu.transaction.infrastructure.adapter.out.gateway;

import com.findu.transaction.application.port.out.gateway.PayoutGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulacion de la transferencia al proveedor: siempre exitosa, referencia {@code MOCK-TRANSFER-<uuid>}.
 * Solo se registra con {@code findu.settlement.mock.enabled=true} (local/dev). En qa/prod, sin
 * adaptador real, una liquidacion con neto positivo falla de forma explicita (no hay pagos falsos).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "findu.settlement.mock.enabled", havingValue = "true")
public class MockPayoutGatewayAdapter implements PayoutGatewayPort {

    public static final String REFERENCE_PREFIX = "MOCK-TRANSFER-";

    @Override
    public PayoutTransferResult transfer(PayoutTransferRequest request) {
        String reference = REFERENCE_PREFIX + UUID.randomUUID();
        log.info("[MOCK] Transferencia simulada providerId={} monto={} settlement={} ref={}",
                request.providerId(), request.amount() != null ? request.amount().getAmount() : null,
                request.settlementCode(), reference);
        return new PayoutTransferResult(true, reference, "Transferencia simulada exitosa");
    }
}
