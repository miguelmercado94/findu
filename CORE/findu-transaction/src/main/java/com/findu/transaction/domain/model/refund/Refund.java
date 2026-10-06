package com.findu.transaction.domain.model.refund;

import com.findu.transaction.domain.enums.RefundStatus;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Refund {

    private String id;
    private String refundCode;
    private String transactionId;
    private Long customerId;
    private Money amount;
    private String reason;
    private RefundStatus status;
    private String externalReference;
    private String failureReason;

    private Instant createdAt;
    private Instant processedAt;
    private Instant completedAt;

    private Refund() {}

    public static Refund create(String transactionId, Long customerId, Money amount, String reason) {
        if (transactionId == null || transactionId.isBlank()) throw new IllegalArgumentException("El transactionId es obligatorio");
        if (customerId == null) throw new IllegalArgumentException("El customerId es obligatorio");
        if (amount == null || !amount.isPositive()) throw new IllegalArgumentException("El monto a devolver debe ser positivo");

        Refund refund = new Refund();
        refund.id = UUID.randomUUID().toString();
        refund.refundCode = "REF-" + Instant.now().toEpochMilli();
        refund.transactionId = transactionId;
        refund.customerId = customerId;
        refund.amount = amount;
        refund.reason = reason != null ? reason.trim() : "";
        refund.status = RefundStatus.PENDING;
        refund.createdAt = Instant.now();

        return refund;
    }

    public static Refund reconstitute(
            String id,
            String refundCode,
            String transactionId,
            Long customerId,
            Money amount,
            String reason,
            RefundStatus status,
            String externalReference,
            String failureReason,
            Instant createdAt,
            Instant processedAt,
            Instant completedAt) {

        Refund r = new Refund();
        r.id = id;
        r.refundCode = refundCode;
        r.transactionId = transactionId;
        r.customerId = customerId;
        r.amount = amount;
        r.reason = reason;
        r.status = status;
        r.externalReference = externalReference;
        r.failureReason = failureReason;
        r.createdAt = createdAt;
        r.processedAt = processedAt;
        r.completedAt = completedAt;
        return r;
    }

    public void markProcessing() {
        this.status = RefundStatus.PROCESSING;
        this.processedAt = Instant.now();
    }

    public void markCompleted(String externalReference) {
        this.status = RefundStatus.COMPLETED;
        this.externalReference = externalReference != null ? externalReference : "SIMULATED-REFUND";
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = RefundStatus.FAILED;
        this.failureReason = reason != null ? reason : "Error al procesar devolución";
    }
}
