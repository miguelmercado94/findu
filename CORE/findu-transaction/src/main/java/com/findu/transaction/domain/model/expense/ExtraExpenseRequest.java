package com.findu.transaction.domain.model.expense;

import com.findu.transaction.domain.enums.ExtraExpenseStatus;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ExtraExpenseRequest {

    private String id;
    private Long serviceRequestId;
    private Long providerId;
    private Money amount;
    private String reason;
    private ExtraExpenseStatus status;

    private Instant requestedAt;
    private Instant approvedAt;
    private Instant updatedAt;

    private ExtraExpenseRequest() {}

    public static ExtraExpenseRequest request(Long serviceRequestId, Long providerId, Money amount, String reason) {
        if (serviceRequestId == null) throw new IllegalArgumentException("El serviceRequestId es obligatorio");
        if (providerId == null) throw new IllegalArgumentException("El providerId es obligatorio");
        if (amount == null || !amount.isPositive()) throw new IllegalArgumentException("El monto del gasto extra debe ser positivo");

        ExtraExpenseRequest req = new ExtraExpenseRequest();
        req.id = "EXP-" + UUID.randomUUID().toString().substring(0, 8);
        req.serviceRequestId = serviceRequestId;
        req.providerId = providerId;
        req.amount = amount;
        req.reason = reason != null ? reason.trim() : "";
        req.status = ExtraExpenseStatus.REQUESTED;

        Instant now = Instant.now();
        req.requestedAt = now;
        req.updatedAt = now;

        return req;
    }

    public static ExtraExpenseRequest reconstitute(
            String id,
            Long serviceRequestId,
            Long providerId,
            Money amount,
            String reason,
            ExtraExpenseStatus status,
            Instant requestedAt,
            Instant approvedAt,
            Instant updatedAt) {

        ExtraExpenseRequest req = new ExtraExpenseRequest();
        req.id = id;
        req.serviceRequestId = serviceRequestId;
        req.providerId = providerId;
        req.amount = amount;
        req.reason = reason;
        req.status = status;
        req.requestedAt = requestedAt;
        req.approvedAt = approvedAt;
        req.updatedAt = updatedAt;
        return req;
    }

    public void approve() {
        if (this.status != ExtraExpenseStatus.REQUESTED) {
            throw new IllegalStateException("Solo se pueden aprobar gastos extras en estado REQUESTED. Estado actual: " + this.status);
        }
        Instant now = Instant.now();
        this.status = ExtraExpenseStatus.APPROVED;
        this.approvedAt = now;
        this.updatedAt = now;
    }

    public void reject() {
        if (this.status != ExtraExpenseStatus.REQUESTED) {
            throw new IllegalStateException("Solo se pueden rechazar gastos extras en estado REQUESTED.");
        }
        this.status = ExtraExpenseStatus.REJECTED;
        this.updatedAt = Instant.now();
    }
}
