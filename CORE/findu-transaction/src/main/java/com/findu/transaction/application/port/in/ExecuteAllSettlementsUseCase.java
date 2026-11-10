package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.dto.SettlementBatchResult;

import java.time.LocalDate;

public interface ExecuteAllSettlementsUseCase {

    /** Liquida a todos los proveedores con cuenta; el fallo de uno no corta el lote. */
    SettlementBatchResult executeAll(LocalDate cutoffDate);
}
