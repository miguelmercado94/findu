package com.findu.transaction.infrastructure.adapter.out.transfer;

import com.findu.transaction.application.port.out.transfer.TransferPort;
import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.out.client.ValidationBrebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimulatedTransferAdapter implements TransferPort {

    private final ValidationBrebClient validationBrebClient;

    @Override
    public TransferResult transfer(String referenceId, Long recipientId, Money amount, TransferSimulationOutcome desiredOutcome) {
        if (validationBrebClient != null && recipientId != null) {
            validationBrebClient.getActiveBrebKey(recipientId).ifPresent(key -> {
                log.info("Dispersión dirigida a la cuenta bancaria / llave Bre-B activa [Provider: {}, Banco: {}, Tipo: {}, Llave: {}]",
                        recipientId, key.getBankName(), key.getKeyType(), key.getMaskedKeyValue());
            });
        }

        log.info("Simulando transferencia financiera [Ref: {}, Recipient: {}, Monto: {}, Outcome: {}]",
                referenceId, recipientId, amount, desiredOutcome);

        TransferSimulationOutcome outcome = desiredOutcome != null ? desiredOutcome : TransferSimulationOutcome.SUCCESS;

        switch (outcome) {
            case SUCCESS -> {
                String externalId = "SIM-TRX-" + UUID.randomUUID().toString().substring(0, 8);
                return new TransferResult(true, externalId, "COMPLETED", "Transferencia simulada ejecutada con éxito");
            }
            case FAILED -> {
                return new TransferResult(false, null, "FAILED", "Transferencia simulada fallida: Fondos insuficientes en canal");
            }
            case TIMEOUT -> {
                return new TransferResult(false, null, "TIMEOUT", "Timeout simulado al conectar con el procesador de pagos");
            }
            case PENDING -> {
                return new TransferResult(false, null, "PENDING", "Transferencia en estado pendiente de aprobación");
            }
            default -> {
                return new TransferResult(true, "SIM-TRX-DEFAULT", "COMPLETED", "Éxito por defecto");
            }
        }
    }
}
