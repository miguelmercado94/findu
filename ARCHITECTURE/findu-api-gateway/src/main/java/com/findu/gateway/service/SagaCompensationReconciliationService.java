package com.findu.gateway.service;

import com.findu.gateway.dto.PendingCompensation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class SagaCompensationReconciliationService {

    private final SagaPendingCompensationStore store;
    private final SagaOrchestratorService orchestrator;

    public SagaCompensationReconciliationService(SagaPendingCompensationStore store,
                                                SagaOrchestratorService orchestrator) {
        this.store = store;
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelay = 60000)
    public void reconcilePendingCompensations() {
        store.getPendingCompensations()
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.empty();
                    }
                    log.info("[Saga Reconciliation] Found {} pending compensations to reconcile", list.size());
                    return Mono.when(list.stream().map(this::reconcileOne).toList());
                })
                .subscribe();
    }

    private Mono<Void> reconcileOne(PendingCompensation comp) {
        log.info("[Saga Reconciliation] Reconciling compensation id={} action={} targetId={}",
                comp.compensationId(), comp.action(), comp.targetId());

        Mono<Boolean> actionMono;
        if ("SECURITY".equalsIgnoreCase(comp.targetService()) && "DELETE_USER".equalsIgnoreCase(comp.action())) {
            actionMono = orchestrator.compensateC1(comp.targetId(), "reconcile-" + comp.compensationId());
        } else if ("CORE".equalsIgnoreCase(comp.targetService()) && "DELETE_PERFIL".equalsIgnoreCase(comp.action())) {
            actionMono = orchestrator.compensateC2(comp.targetId(), "reconcile-" + comp.compensationId());
        } else {
            log.warn("Unknown pending compensation action: {}", comp);
            return store.removePendingCompensation(comp.compensationId());
        }

        return actionMono.flatMap(success -> {
            if (Boolean.TRUE.equals(success)) {
                log.info("[Saga Reconciliation] Successfully reconciled and removed compensation id={}", comp.compensationId());
                return store.removePendingCompensation(comp.compensationId());
            } else {
                log.warn("[Saga Reconciliation] Reconciliation attempt failed for compensation id={}, will retry on next cycle",
                        comp.compensationId());
                return Mono.empty();
            }
        });
    }
}
