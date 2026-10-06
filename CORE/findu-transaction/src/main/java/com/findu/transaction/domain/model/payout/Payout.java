package com.findu.transaction.domain.model.payout;

import com.findu.transaction.domain.enums.PayoutStatus;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Payout {

    private String id;
    private String payoutCode;
    private String settlementId;
    private Long providerId;
    private Money amount;
    private PayoutStatus status;
    private String externalReference;
    private String failureReason;

    private Instant createdAt;
    private Instant processedAt;
    private Instant completedAt;

    private Payout() {}

    public static Payout create(String settlementId, Long providerId, Money amount) {
        if (providerId == null) throw new IllegalArgumentException("El providerId es obligatorio");
        if (amount == null || !amount.isPositive()) throw new IllegalArgumentException("El monto del payout debe ser positivo");

        Payout payout = new Payout();
        payout.id = UUID.randomUUID().toString();
        payout.payoutCode = "PO-" + Instant.now().toEpochMilli();
        payout.settlementId = settlementId != null ? settlementId : "";
        payout.providerId = providerId;
        payout.amount = amount;
        payout.status = PayoutStatus.PENDING;
        payout.createdAt = Instant.now();

        return payout;
    }

    public static Payout reconstitute(
            String id,
            String payoutCode,
            String settlementId,
            Long providerId,
            Money amount,
            PayoutStatus status,
            String externalReference,
            String failureReason,
            Instant createdAt,
            Instant processedAt,
            Instant completedAt) {

        Payout p = new Payout();
        p.id = id;
        p.payoutCode = payoutCode;
        p.settlementId = settlementId;
        p.providerId = providerId;
        p.amount = amount;
        p.status = status;
        p.externalReference = externalReference;
        p.failureReason = failureReason;
        p.createdAt = createdAt;
        p.processedAt = processedAt;
        p.completedAt = completedAt;
        return p;
    }

    public void markProcessing() {
        if (this.status == PayoutStatus.COMPLETED) {
            throw new IllegalStateException("El payout ya fue completado.");
        }
        this.status = PayoutStatus.PROCESSING;
        this.processedAt = Instant.now();
    }

    public void markCompleted(String externalReference) {
        this.status = PayoutStatus.COMPLETED;
        this.externalReference = externalReference != null ? externalReference : "SIMULATED-PAYOUT";
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PayoutStatus.FAILED;
        this.failureReason = reason != null ? reason : "Error desconocido en transferencia";
    }
}
