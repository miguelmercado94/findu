package com.findu.transaction.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** Resultado agregado de liquidar a todos los proveedores. */
@Getter
@Builder
public class SettlementBatchResult {
    private final LocalDate cutoffDate;
    private final List<SettlementRunResult> results;

    public long count(SettlementRunResult.Outcome... outcomes) {
        List<SettlementRunResult.Outcome> wanted = List.of(outcomes);
        return results.stream().filter(r -> wanted.contains(r.getOutcome())).count();
    }
}
