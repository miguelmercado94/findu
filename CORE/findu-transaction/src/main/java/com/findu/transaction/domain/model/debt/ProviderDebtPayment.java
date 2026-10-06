package com.findu.transaction.domain.model.debt;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.enums.PaymentStatus;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ProviderDebtPayment {

    private String id;
    private String paymentCode;
    private Long providerId;
    private Money amount;
    private PaymentMethod paymentMethod;
    private String externalReference;
    private PaymentStatus status;
    private Instant createdAt;

    private ProviderDebtPayment() {}

    public static ProviderDebtPayment create(Long providerId, Money amount, PaymentMethod paymentMethod, String externalReference) {
        if (providerId == null) throw new IllegalArgumentException("El providerId es obligatorio");
        if (amount == null || !amount.isPositive()) throw new IllegalArgumentException("El monto a abonar a la deuda debe ser positivo");

        ProviderDebtPayment p = new ProviderDebtPayment();
        p.id = UUID.randomUUID().toString();
        p.paymentCode = "DEBT-PAY-" + Instant.now().toEpochMilli();
        p.providerId = providerId;
        p.amount = amount;
        p.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.SIMULATED;
        p.externalReference = externalReference != null ? externalReference : "SIMULATED-DEBT-PAYMENT";
        p.status = PaymentStatus.COMPLETED;
        p.createdAt = Instant.now();

        return p;
    }

    public static ProviderDebtPayment reconstitute(
            String id,
            String paymentCode,
            Long providerId,
            Money amount,
            PaymentMethod paymentMethod,
            String externalReference,
            PaymentStatus status,
            Instant createdAt) {

        ProviderDebtPayment p = new ProviderDebtPayment();
        p.id = id;
        p.paymentCode = paymentCode;
        p.providerId = providerId;
        p.amount = amount;
        p.paymentMethod = paymentMethod;
        p.externalReference = externalReference;
        p.status = status;
        p.createdAt = createdAt;
        return p;
    }
}
