package com.findu.transaction.domain.model.account;

import com.findu.transaction.domain.enums.*;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class ProviderAccount {

    private String id;
    private Long providerId;
    private ProviderType providerType;
    private Money payableBalance;   // Dinero que FindU debe al proveedor
    private Money receivableBalance; // Dinero que el proveedor debe a FindU (deuda)
    private Money debtLimit;         // Límite de deuda (COP 100k para Persona Natural, 500k para Corporativo)
    private ProviderAccountStatus status;

    private final List<ProviderLedgerEntry> ledgerEntries = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    private ProviderAccount() {}

    public static ProviderAccount create(Long providerId, ProviderType providerType) {
        if (providerId == null) throw new IllegalArgumentException("El providerId es obligatorio");
        ProviderType type = providerType != null ? providerType : ProviderType.PERSONA_NATURAL;

        ProviderAccount account = new ProviderAccount();
        account.id = "ACC-" + providerId;
        account.providerId = providerId;
        account.providerType = type;
        account.payableBalance = Money.ZERO;
        account.receivableBalance = Money.ZERO;
        account.debtLimit = Money.of(type.getDebtLimitCop());
        account.status = ProviderAccountStatus.NORMAL;

        Instant now = Instant.now();
        account.createdAt = now;
        account.updatedAt = now;

        return account;
    }

    public static ProviderAccount reconstitute(
            String id,
            Long providerId,
            ProviderType providerType,
            Money payableBalance,
            Money receivableBalance,
            Money debtLimit,
            ProviderAccountStatus status,
            List<ProviderLedgerEntry> entries,
            Instant createdAt,
            Instant updatedAt) {

        ProviderAccount account = new ProviderAccount();
        account.id = id;
        account.providerId = providerId;
        account.providerType = providerType;
        account.payableBalance = payableBalance;
        account.receivableBalance = receivableBalance;
        account.debtLimit = debtLimit;
        account.status = status;
        if (entries != null) {
            account.ledgerEntries.addAll(entries);
        }
        account.createdAt = createdAt;
        account.updatedAt = updatedAt;
        return account;
    }

    /**
     * Procesa el resultado financiero de una transacción de servicio.
     */
    public void recordServiceTransaction(ServiceTransaction tx) {
        if (tx == null) throw new IllegalArgumentException("La transacción no puede ser nula");
        if (!tx.getProviderId().equals(this.providerId)) {
            throw new IllegalArgumentException("La transacción no pertenece a este proveedor");
        }

        if (tx.getPaymentMethod() == PaymentMethod.CASH) {
            // El proveedor recibió todo el dinero en efectivo. Debe la comisión a FindU.
            Money commissionDebt = tx.getCommissionAmount();
            this.receivableBalance = this.receivableBalance.add(commissionDebt);
            addLedgerEntry(LedgerEntryType.CASH_PAYMENT_DEBT, commissionDebt, LedgerDirection.DEBIT,
                    tx.getTransactionCode().getValue(), "Deuda de comisión por servicio pagado en efectivo");
        } else {
            // El dinero ingresó a FindU por transferencia/medio digital.
            // FindU le debe al proveedor su ganancia neta.
            Money netEarnings = tx.getProviderAmount();
            this.payableBalance = this.payableBalance.add(netEarnings);
            addLedgerEntry(LedgerEntryType.SERVICE_EARNING, netEarnings, LedgerDirection.CREDIT,
                    tx.getTransactionCode().getValue(), "Ganancia neta por servicio pagado digitalmente");
        }

        checkRestriction();
        this.updatedAt = Instant.now();
    }

    /**
     * Aplica la compensación de deudas: Cruza la deuda del proveedor contra lo que FindU le debe.
     * Retorna el monto de la compensación aplicada.
     */
    public Money applyCompensation() {
        if (this.payableBalance.isPositive() && this.receivableBalance.isPositive()) {
            BigDecimal compensationAmount = this.payableBalance.getAmount().min(this.receivableBalance.getAmount());
            Money compensation = new Money(compensationAmount, this.payableBalance.getCurrency());

            this.payableBalance = this.payableBalance.subtract(compensation);
            this.receivableBalance = this.receivableBalance.subtract(compensation);

            addLedgerEntry(LedgerEntryType.COMPENSATION_ADJUSTMENT, compensation, LedgerDirection.DEBIT,
                    "COMP-" + Instant.now().toEpochMilli(), "Compensación de deuda del proveedor contra saldo a favor");

            checkRestriction();
            this.updatedAt = Instant.now();
            return compensation;
        }
        return Money.ZERO;
    }

    /**
     * Registra un pago de deuda realizado por el proveedor a FindU.
     */
    public void recordDebtPayment(Money amount, String paymentReference) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("El monto a pagar de la deuda debe ser positivo");
        }

        this.receivableBalance = this.receivableBalance.subtract(amount);
        if (this.receivableBalance.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            this.receivableBalance = Money.ZERO;
        }

        addLedgerEntry(LedgerEntryType.DEBT_PAYMENT, amount, LedgerDirection.DEBIT,
                paymentReference, "Pago registrado de deuda de proveedor a FindU");

        checkRestriction();
        this.updatedAt = Instant.now();
    }

    /**
     * Registra un Payout (pago de FindU hacia el proveedor).
     */
    public void recordPayout(Money amount, String payoutId) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("El monto de payout debe ser positivo");
        }
        if (amount.isGreaterThan(this.payableBalance)) {
            throw new IllegalStateException("El monto del payout excede el saldo a favor disponible: " + this.payableBalance);
        }

        this.payableBalance = this.payableBalance.subtract(amount);
        addLedgerEntry(LedgerEntryType.PAYOUT, amount, LedgerDirection.DEBIT,
                payoutId, "Payout transferido al proveedor");

        this.updatedAt = Instant.now();
    }

    public void checkRestriction() {
        if (this.receivableBalance.isGreaterThan(this.debtLimit)) {
            this.status = ProviderAccountStatus.RESTRICTED;
        } else {
            this.status = ProviderAccountStatus.NORMAL;
        }
    }

    private void addLedgerEntry(LedgerEntryType type, Money amount, LedgerDirection direction, String refId, String desc) {
        this.ledgerEntries.add(new ProviderLedgerEntry(this.providerId, type, amount, direction, refId, desc));
    }

    public List<ProviderLedgerEntry> getLedgerEntries() {
        return Collections.unmodifiableList(ledgerEntries);
    }
}
