package com.findu.transaction.domain.model.transaction;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.enums.PaymentStatus;
import com.findu.transaction.domain.enums.TransactionStatus;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.domain.valueobject.TransactionCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class ServiceTransaction {

    private String id;
    private TransactionCode transactionCode;
    private Long serviceRequestId;
    private Long providerId;
    private Long customerId;

    private Money serviceAmount;
    private BigDecimal commissionRate;
    private Money commissionAmount;
    private Money tipAmount;
    private Money extraAmount;
    private Money totalCustomerAmount;
    private Money providerAmount;

    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private TransactionStatus transactionStatus;

    private Instant serviceCompletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private ServiceTransaction() {}

    public static ServiceTransaction create(
            Long serviceRequestId,
            Long providerId,
            Long customerId,
            Money serviceAmount,
            BigDecimal commissionRate,
            Money tipAmount,
            Money extraAmount,
            PaymentMethod paymentMethod) {

        if (serviceRequestId == null) throw new IllegalArgumentException("El id de solicitud de servicio es obligatorio");
        if (providerId == null) throw new IllegalArgumentException("El id de proveedor es obligatorio");
        if (customerId == null) throw new IllegalArgumentException("El id de cliente es obligatorio");
        if (serviceAmount == null || !serviceAmount.isPositive()) throw new IllegalArgumentException("El monto del servicio debe ser positivo");
        if (paymentMethod == null) throw new IllegalArgumentException("El método de pago es obligatorio");

        BigDecimal rate = commissionRate != null ? commissionRate : new BigDecimal("0.10"); // Default 10%
        Money tip = tipAmount != null ? tipAmount : Money.ZERO;
        Money extra = extraAmount != null ? extraAmount : Money.ZERO;

        Money commission = serviceAmount.multiply(rate);
        Money totalCustomer = serviceAmount.add(tip).add(extra);
        Money providerEarnings = serviceAmount.subtract(commission).add(tip).add(extra);

        ServiceTransaction tx = new ServiceTransaction();
        tx.id = UUID.randomUUID().toString();
        tx.transactionCode = TransactionCode.generate("TX");
        tx.serviceRequestId = serviceRequestId;
        tx.providerId = providerId;
        tx.customerId = customerId;

        tx.serviceAmount = serviceAmount;
        tx.commissionRate = rate;
        tx.commissionAmount = commission;
        tx.tipAmount = tip;
        tx.extraAmount = extra;
        tx.totalCustomerAmount = totalCustomer;
        tx.providerAmount = providerEarnings;

        tx.paymentMethod = paymentMethod;
        tx.paymentStatus = PaymentStatus.COMPLETED; // Inicialmente el pago es aceptado
        tx.transactionStatus = TransactionStatus.REGISTERED;

        Instant now = Instant.now();
        tx.serviceCompletedAt = now;
        tx.createdAt = now;
        tx.updatedAt = now;

        return tx;
    }

    public static ServiceTransaction reconstitute(
            String id,
            TransactionCode transactionCode,
            Long serviceRequestId,
            Long providerId,
            Long customerId,
            Money serviceAmount,
            BigDecimal commissionRate,
            Money commissionAmount,
            Money tipAmount,
            Money extraAmount,
            Money totalCustomerAmount,
            Money providerAmount,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            TransactionStatus transactionStatus,
            Instant serviceCompletedAt,
            Instant createdAt,
            Instant updatedAt) {

        ServiceTransaction tx = new ServiceTransaction();
        tx.id = id;
        tx.transactionCode = transactionCode;
        tx.serviceRequestId = serviceRequestId;
        tx.providerId = providerId;
        tx.customerId = customerId;
        tx.serviceAmount = serviceAmount;
        tx.commissionRate = commissionRate;
        tx.commissionAmount = commissionAmount;
        tx.tipAmount = tipAmount;
        tx.extraAmount = extraAmount;
        tx.totalCustomerAmount = totalCustomerAmount;
        tx.providerAmount = providerAmount;
        tx.paymentMethod = paymentMethod;
        tx.paymentStatus = paymentStatus;
        tx.transactionStatus = transactionStatus;
        tx.serviceCompletedAt = serviceCompletedAt;
        tx.createdAt = createdAt;
        tx.updatedAt = updatedAt;
        return tx;
    }

    public void cancel() {
        if (this.transactionStatus == TransactionStatus.SETTLED) {
            throw new IllegalStateException("No se puede cancelar una transacción ya liquidada.");
        }
        this.transactionStatus = TransactionStatus.CANCELLED;
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void markSettled() {
        this.transactionStatus = TransactionStatus.SETTLED;
        this.updatedAt = Instant.now();
    }
}
