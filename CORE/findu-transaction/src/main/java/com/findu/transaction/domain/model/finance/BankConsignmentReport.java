package com.findu.transaction.domain.model.finance;

import com.findu.transaction.domain.enums.PaymentStatus;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class BankConsignmentReport {

    private String id;
    private Long providerId;
    private Money amount;
    private String reference;
    private String bankName;
    private PaymentStatus status;
    private Instant confirmedAt;
    private String confirmedBy;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    private BankConsignmentReport() {}

    public static BankConsignmentReport create(
            Long providerId,
            Money amount,
            String reference,
            String bankName,
            String notes) {

        if (providerId == null) throw new IllegalArgumentException("providerId es obligatorio");
        if (amount == null || !amount.isPositive()) throw new IllegalArgumentException("El monto debe ser positivo");
        if (reference == null || reference.isBlank()) throw new IllegalArgumentException("La referencia de pago es obligatoria");

        Instant now = Instant.now();
        BankConsignmentReport report = new BankConsignmentReport();
        report.id = UUID.randomUUID().toString();
        report.providerId = providerId;
        report.amount = amount;
        report.reference = reference.trim();
        report.bankName = bankName != null ? bankName.trim() : "Bancolombia";
        report.status = PaymentStatus.PENDING;
        report.notes = notes != null ? notes.trim() : "";
        report.createdAt = now;
        report.updatedAt = now;

        return report;
    }

    public static BankConsignmentReport reconstitute(
            String id,
            Long providerId,
            Money amount,
            String reference,
            String bankName,
            PaymentStatus status,
            Instant confirmedAt,
            String confirmedBy,
            String notes,
            Instant createdAt,
            Instant updatedAt) {

        BankConsignmentReport report = new BankConsignmentReport();
        report.id = id;
        report.providerId = providerId;
        report.amount = amount;
        report.reference = reference;
        report.bankName = bankName;
        report.status = status;
        report.confirmedAt = confirmedAt;
        report.confirmedBy = confirmedBy;
        report.notes = notes;
        report.createdAt = createdAt;
        report.updatedAt = updatedAt;
        return report;
    }

    public void confirm(String confirmedBy) {
        if (this.status == PaymentStatus.COMPLETED) {
            return; // Idempotente
        }
        this.status = PaymentStatus.COMPLETED;
        this.confirmedBy = confirmedBy != null ? confirmedBy : "SYSTEM_RECONCILIATION";
        this.confirmedAt = Instant.now();
        this.updatedAt = this.confirmedAt;
    }

    public void reject(String reason) {
        this.status = PaymentStatus.FAILED;
        this.notes = (this.notes != null ? this.notes + " | " : "") + "Rechazado: " + reason;
        this.updatedAt = Instant.now();
    }
}
