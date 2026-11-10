package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.CreateSettlementUseCase;
import com.findu.transaction.application.port.in.ExecuteAllSettlementsUseCase;
import com.findu.transaction.application.port.in.ExecuteSettlementUseCase;
import com.findu.transaction.application.port.in.GetSettlementUseCase;
import com.findu.transaction.application.port.in.command.CreateSettlementCommand;
import com.findu.transaction.application.port.in.dto.SettlementBatchResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult.Outcome;
import com.findu.transaction.application.port.in.query.GetSettlementQuery;
import com.findu.transaction.application.port.out.persistence.SettlementRepository;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.domain.service.SettlementSchedule;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.SettlementExecutionResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fachada de liquidaciones. La logica de calculo y pago vive en {@link SettlementExecutionService}
 * (un proveedor) y {@link SettlementBatchService} (todos); aqui solo se delega y se adapta el resultado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageSettlementService implements CreateSettlementUseCase, GetSettlementUseCase {

    private final SettlementRepository settlementRepository;
    private final ExecuteSettlementUseCase executeSettlementUseCase;
    private final ExecuteAllSettlementsUseCase executeAllSettlementsUseCase;
    private final Clock clock;

    /**
     * Liquida al proveedor del comando. {@code targetDate} se interpreta como fecha de ejecucion
     * (el corte es el domingo anterior); sin fecha se usa hoy. Si no hay nada que liquidar o ya se
     * liquido ese corte, devuelve la ultima liquidacion existente o una SKIPPED en memoria (sin persistir).
     */
    @Override
    public Settlement execute(CreateSettlementCommand command) {
        if (command == null || command.getProviderId() == null) {
            throw new IllegalArgumentException("El comando de settlement y el providerId son obligatorios");
        }
        Long providerId = command.getProviderId();
        LocalDate execDate = command.getTargetDate() != null ? command.getTargetDate() : command.getEndDate();
        LocalDate cutoff = execDate != null ? SettlementSchedule.cutoffDate(execDate) : null;

        SettlementRunResult result = executeSettlementUseCase.executeForProvider(providerId, cutoff);
        if (result.getSettlementId() != null) {
            Optional<Settlement> found = settlementRepository.findById(result.getSettlementId());
            if (found.isPresent()) {
                return found.get();
            }
        }
        return settlementRepository.findLatestCompletedByProviderId(providerId).orElseGet(() -> {
            Settlement skipped = Settlement.create(providerId, null, SettlementType.WEEKLY,
                    Money.ZERO, Money.ZERO, null, null, null);
            skipped.markAsSkipped();
            return skipped;
        });
    }

    /**
     * Liquida a todos los proveedores. El parametro {@code type} se ignora (la liquidacion es siempre
     * semanal: corte domingo, pago martes); se conserva por compatibilidad con /internal/settlements/run.
     */
    public SettlementExecutionResultResponse runBatchSettlements(LocalDate targetDate, String type) {
        LocalDate cutoff = targetDate != null ? SettlementSchedule.cutoffDate(targetDate) : null;
        return toResponse(executeAllSettlementsUseCase.executeAll(cutoff));
    }

    /**
     * Disparo manual: un proveedor ({@code providerId != null}) o todos. {@code cutoffDate} opcional
     * (por defecto el domingo anterior a hoy).
     */
    public SettlementExecutionResultResponse executeManual(Long providerId, LocalDate cutoffDate) {
        if (providerId == null) {
            return toResponse(executeAllSettlementsUseCase.executeAll(cutoffDate));
        }
        LocalDate cutoff = cutoffDate != null ? cutoffDate : SettlementSchedule.cutoffDate(LocalDate.now(clock));
        SettlementRunResult one = executeSettlementUseCase.executeForProvider(providerId, cutoff);
        return toResponse(SettlementBatchResult.builder().cutoffDate(cutoff).results(List.of(one)).build());
    }

    private SettlementExecutionResultResponse toResponse(SettlementBatchResult batch) {
        Instant startedAt = Instant.now();
        List<String> errors = new ArrayList<>();
        List<SettlementExecutionResultResponse.ProviderResult> results = new ArrayList<>();
        for (SettlementRunResult r : batch.getResults()) {
            if (r.getOutcome() == Outcome.FAILED) {
                errors.add(String.format("Error liquidando providerId=%d: %s", r.getProviderId(), r.getMessage()));
            }
            results.add(SettlementExecutionResultResponse.ProviderResult.builder()
                    .providerId(r.getProviderId())
                    .outcome(r.getOutcome().name())
                    .settlementId(r.getSettlementId())
                    .settlementCode(r.getSettlementCode())
                    .grossAmount(r.getGrossAmount())
                    .debtCompensationAmount(r.getDebtCompensationAmount())
                    .netAmount(r.getNetAmount())
                    .remainingDebt(r.getRemainingDebt())
                    .transferred(r.isTransferred())
                    .payoutCode(r.getPayoutCode())
                    .reference(r.getReference())
                    .periodStart(r.getPeriodStart())
                    .periodEnd(r.getPeriodEnd())
                    .executedAt(r.getExecutedAt())
                    .dailyBalancesSettled(r.getDailyBalancesSettled())
                    .message(r.getMessage())
                    .build());
        }
        int created = (int) batch.count(Outcome.SETTLED, Outcome.NET_ZERO_NO_TRANSFER);
        int skipped = (int) batch.count(Outcome.ALREADY_SETTLED, Outcome.NOTHING_TO_SETTLE);
        int failed = (int) batch.count(Outcome.FAILED);
        String status = failed == 0 ? "COMPLETED" : (created > 0 ? "PARTIAL_SUCCESS" : "FAILED");
        return SettlementExecutionResultResponse.builder()
                .executionId("exec-set-" + UUID.randomUUID().toString().substring(0, 8))
                .targetDate(batch.getCutoffDate())
                .cutoffDate(batch.getCutoffDate())
                .nextSettlementDate(SettlementSchedule.nextTuesdayAfter(LocalDate.now(clock)))
                .providersProcessed(batch.getResults().size())
                .settlementsCreated(created)
                .settlementsSkipped(skipped)
                .providersFailed(failed)
                .startedAt(startedAt)
                .finishedAt(Instant.now())
                .status(status)
                .errors(errors)
                .results(results)
                .build();
    }

    @Override
    public Optional<Settlement> execute(GetSettlementQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        if (query.getSettlementId() != null) {
            return settlementRepository.findById(query.getSettlementId());
        }
        if (query.getProviderId() != null) {
            List<Settlement> list = settlementRepository.findByProviderId(query.getProviderId());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
        }
        return Optional.empty();
    }
}
