package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.ExecuteAllSettlementsUseCase;
import com.findu.transaction.application.port.in.ExecuteSettlementUseCase;
import com.findu.transaction.application.port.in.dto.SettlementBatchResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult.Outcome;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.service.SettlementSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Liquida a todos los proveedores. NO es transaccional: cada proveedor corre en su propia transaccion
 * (via el proxy de {@link ExecuteSettlementUseCase}) para que el fallo de uno no revierta ni corte al resto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchService implements ExecuteAllSettlementsUseCase {

    private final ProviderAccountRepository providerAccountRepository;
    private final ExecuteSettlementUseCase executeSettlementUseCase;
    private final Clock clock;

    @Override
    public SettlementBatchResult executeAll(LocalDate cutoffDate) {
        LocalDate cutoff = cutoffDate != null ? cutoffDate : SettlementSchedule.cutoffDate(LocalDate.now(clock));
        List<Long> providerIds = providerAccountRepository.findAll().stream()
                .map(ProviderAccount::getProviderId)
                .distinct()
                .toList();
        List<SettlementRunResult> results = new ArrayList<>();
        for (Long providerId : providerIds) {
            try {
                results.add(executeSettlementUseCase.executeForProvider(providerId, cutoff));
            } catch (Exception e) {
                log.error("Error liquidando providerId={}: {}", providerId, e.getMessage(), e);
                results.add(SettlementRunResult.builder()
                        .providerId(providerId)
                        .outcome(Outcome.FAILED)
                        .message(e.getMessage())
                        .build());
            }
        }
        return SettlementBatchResult.builder().cutoffDate(cutoff).results(results).build();
    }
}
