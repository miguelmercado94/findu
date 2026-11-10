package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.dto.SettlementRunResult;

import java.time.LocalDate;

public interface ExecuteSettlementUseCase {

    /**
     * Liquida a un proveedor hasta la fecha de corte (domingo). {@code cutoffDate == null} usa el domingo
     * anterior a hoy (America/Bogota). Idempotente: repetir el mismo corte devuelve ALREADY_SETTLED.
     */
    SettlementRunResult executeForProvider(Long providerId, LocalDate cutoffDate);
}
