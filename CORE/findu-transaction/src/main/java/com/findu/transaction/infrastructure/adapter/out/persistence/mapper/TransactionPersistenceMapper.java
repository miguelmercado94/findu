package com.findu.transaction.infrastructure.adapter.out.persistence.mapper;

import com.findu.transaction.domain.enums.*;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.debt.ProviderDebtPayment;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.domain.model.refund.Refund;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.domain.valueobject.TransactionCode;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TransactionPersistenceMapper {

    // ==========================================
    // SERVICE TRANSACTION
    // ==========================================
    public ServiceTransactionEntity toEntity(ServiceTransaction domain) {
        if (domain == null) return null;
        return ServiceTransactionEntity.builder()
                .id(domain.getId())
                .transactionCode(domain.getTransactionCode() != null ? domain.getTransactionCode().getValue() : null)
                .serviceRequestId(domain.getServiceRequestId())
                .providerId(domain.getProviderId())
                .customerId(domain.getCustomerId())
                .serviceAmount(domain.getServiceAmount() != null ? domain.getServiceAmount().getAmount() : null)
                .commissionRate(domain.getCommissionRate())
                .commissionAmount(domain.getCommissionAmount() != null ? domain.getCommissionAmount().getAmount() : null)
                .tipAmount(domain.getTipAmount() != null ? domain.getTipAmount().getAmount() : null)
                .extraAmount(domain.getExtraAmount() != null ? domain.getExtraAmount().getAmount() : null)
                .totalCustomerAmount(domain.getTotalCustomerAmount() != null ? domain.getTotalCustomerAmount().getAmount() : null)
                .providerAmount(domain.getProviderAmount() != null ? domain.getProviderAmount().getAmount() : null)
                .paymentMethod(domain.getPaymentMethod() != null ? domain.getPaymentMethod().name() : null)
                .paymentStatus(domain.getPaymentStatus() != null ? domain.getPaymentStatus().name() : null)
                .transactionStatus(domain.getTransactionStatus() != null ? domain.getTransactionStatus().name() : null)
                .serviceCompletedAt(domain.getServiceCompletedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public ServiceTransaction toDomain(ServiceTransactionEntity entity) {
        if (entity == null) return null;
        return ServiceTransaction.reconstitute(
                entity.getId(),
                entity.getTransactionCode() != null ? new TransactionCode(entity.getTransactionCode()) : null,
                entity.getServiceRequestId(),
                entity.getProviderId(),
                entity.getCustomerId(),
                new Money(entity.getServiceAmount()),
                entity.getCommissionRate(),
                new Money(entity.getCommissionAmount()),
                new Money(entity.getTipAmount()),
                new Money(entity.getExtraAmount()),
                new Money(entity.getTotalCustomerAmount()),
                new Money(entity.getProviderAmount()),
                entity.getPaymentMethod() != null ? PaymentMethod.valueOf(entity.getPaymentMethod()) : null,
                entity.getPaymentStatus() != null ? PaymentStatus.valueOf(entity.getPaymentStatus()) : null,
                entity.getTransactionStatus() != null ? TransactionStatus.valueOf(entity.getTransactionStatus()) : null,
                entity.getServiceCompletedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // ==========================================
    // PROVIDER ACCOUNT & LEDGER
    // ==========================================
    public ProviderAccountEntity toEntity(ProviderAccount domain) {
        if (domain == null) return null;
        return ProviderAccountEntity.builder()
                .id(domain.getId())
                .providerId(domain.getProviderId())
                .providerType(domain.getProviderType() != null ? domain.getProviderType().name() : null)
                .payableBalance(domain.getPayableBalance() != null ? domain.getPayableBalance().getAmount() : null)
                .receivableBalance(domain.getReceivableBalance() != null ? domain.getReceivableBalance().getAmount() : null)
                .lastBalanceDate(null)
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public ProviderAccount toDomain(ProviderAccountEntity entity, List<ProviderLedgerEntryEntity> ledgerEntities) {
        if (entity == null) return null;
        List<ProviderLedgerEntry> ledger = ledgerEntities != null ?
                ledgerEntities.stream().map(this::toDomain).collect(Collectors.toList()) : List.of();

        ProviderType type = entity.getProviderType() != null ? ProviderType.valueOf(entity.getProviderType()) : ProviderType.PERSONA_NATURAL;

        return ProviderAccount.reconstitute(
                entity.getId(),
                entity.getProviderId(),
                type,
                new Money(entity.getPayableBalance()),
                new Money(entity.getReceivableBalance()),
                Money.of(type.getDebtLimitCop()),
                entity.getStatus() != null ? ProviderAccountStatus.valueOf(entity.getStatus()) : ProviderAccountStatus.NORMAL,
                ledger,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProviderLedgerEntryEntity toEntity(ProviderLedgerEntry domain) {
        if (domain == null) return null;
        return ProviderLedgerEntryEntity.builder()
                .id(domain.getEntryId())
                .providerId(domain.getProviderId())
                .transactionId(null)
                .entryType(domain.getType() != null ? domain.getType().name() : null)
                .direction(domain.getDirection() != null ? domain.getDirection().name() : null)
                .amount(domain.getAmount() != null ? domain.getAmount().getAmount() : null)
                .payableBalanceAfter(domain.getAmount() != null ? domain.getAmount().getAmount() : java.math.BigDecimal.ZERO)
                .receivableBalanceAfter(java.math.BigDecimal.ZERO)
                .reference(domain.getReferenceId() != null ? domain.getReferenceId() : "")
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public ProviderLedgerEntry toDomain(ProviderLedgerEntryEntity entity) {
        if (entity == null) return null;
        return new ProviderLedgerEntry(
                entity.getId(),
                entity.getProviderId(),
                entity.getEntryType() != null ? LedgerEntryType.valueOf(entity.getEntryType()) : null,
                new Money(entity.getAmount()),
                entity.getDirection() != null ? LedgerDirection.valueOf(entity.getDirection()) : null,
                entity.getReference(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    // ==========================================
    // DAILY BALANCE
    // ==========================================
    public DailyBalanceEntity toEntity(DailyBalance domain) {
        if (domain == null) return null;
        return DailyBalanceEntity.builder()
                .id(domain.getId())
                .providerId(domain.getProviderId())
                .balanceDate(domain.getBalanceDate())
                .openingBalance(domain.getOpeningBalance() != null ? domain.getOpeningBalance().getAmount() : null)
                .totalEarnings(domain.getTotalEarnings() != null ? domain.getTotalEarnings().getAmount() : null)
                .totalCommissions(domain.getTotalCommissions() != null ? domain.getTotalCommissions().getAmount() : null)
                .totalTips(domain.getTotalTips() != null ? domain.getTotalTips().getAmount() : null)
                .totalExtras(domain.getTotalExtras() != null ? domain.getTotalExtras().getAmount() : null)
                .cashPayments(domain.getCashPayments() != null ? domain.getCashPayments().getAmount() : null)
                .digitalPayments(domain.getDigitalPayments() != null ? domain.getDigitalPayments().getAmount() : null)
                .providerDebt(domain.getProviderDebt() != null ? domain.getProviderDebt().getAmount() : null)
                .pendingPayout(domain.getPendingPayout() != null ? domain.getPendingPayout().getAmount() : null)
                .closingBalance(domain.getClosingBalance() != null ? domain.getClosingBalance().getAmount() : null)
                .status("COMPLETED")
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getCreatedAt())
                .build();
    }

    public DailyBalance toDomain(DailyBalanceEntity entity) {
        if (entity == null) return null;
        return DailyBalance.reconstitute(
                entity.getId(),
                entity.getProviderId(),
                entity.getBalanceDate(),
                new Money(entity.getOpeningBalance()),
                new Money(entity.getTotalEarnings()),
                new Money(entity.getTotalCommissions()),
                new Money(entity.getTotalTips()),
                new Money(entity.getTotalExtras()),
                new Money(entity.getCashPayments()),
                new Money(entity.getDigitalPayments()),
                new Money(entity.getProviderDebt()),
                new Money(entity.getPendingPayout()),
                new Money(entity.getClosingBalance()),
                entity.getCreatedAt()
        );
    }

    // ==========================================
    // PAYOUT
    // ==========================================
    public PayoutEntity toEntity(Payout domain) {
        if (domain == null) return null;
        return PayoutEntity.builder()
                .id(domain.getId())
                .payoutCode(domain.getPayoutCode())
                .settlementId(domain.getSettlementId())
                .providerId(domain.getProviderId())
                .amount(domain.getAmount() != null ? domain.getAmount().getAmount() : null)
                .externalReference(domain.getExternalReference())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .failureReason(domain.getFailureReason())
                .executedAt(domain.getCompletedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getCompletedAt() != null ? domain.getCompletedAt() : domain.getCreatedAt())
                .build();
    }

    public Payout toDomain(PayoutEntity entity) {
        if (entity == null) return null;
        return Payout.reconstitute(
                entity.getId(),
                entity.getPayoutCode(),
                entity.getSettlementId(),
                entity.getProviderId(),
                new Money(entity.getAmount()),
                entity.getStatus() != null ? PayoutStatus.valueOf(entity.getStatus()) : null,
                entity.getExternalReference(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getExecutedAt(),
                entity.getExecutedAt()
        );
    }

    // ==========================================
    // REFUND
    // ==========================================
    public RefundEntity toEntity(Refund domain) {
        if (domain == null) return null;
        return RefundEntity.builder()
                .id(domain.getId())
                .refundCode(domain.getRefundCode())
                .transactionId(domain.getTransactionId())
                .customerId(domain.getCustomerId())
                .amount(domain.getAmount() != null ? domain.getAmount().getAmount() : null)
                .reason(domain.getReason())
                .externalReference(domain.getExternalReference())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .failureReason(domain.getFailureReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getCompletedAt() != null ? domain.getCompletedAt() : domain.getCreatedAt())
                .processedAt(domain.getCompletedAt())
                .build();
    }

    public Refund toDomain(RefundEntity entity) {
        if (entity == null) return null;
        return Refund.reconstitute(
                entity.getId(),
                entity.getRefundCode(),
                entity.getTransactionId(),
                entity.getCustomerId(),
                new Money(entity.getAmount()),
                entity.getReason(),
                entity.getStatus() != null ? RefundStatus.valueOf(entity.getStatus()) : null,
                entity.getExternalReference(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getProcessedAt()
        );
    }

    // ==========================================
    // SETTLEMENT
    // ==========================================
    public SettlementEntity toEntity(Settlement domain) {
        if (domain == null) return null;
        return SettlementEntity.builder()
                .id(domain.getId())
                .settlementCode(domain.getSettlementCode())
                .providerId(domain.getProviderId())
                .providerType(domain.getProviderType() != null ? domain.getProviderType().name() : null)
                .grossAmount(domain.getGrossAmount() != null ? domain.getGrossAmount().getAmount() : null)
                .debtCompensationAmount(domain.getDebtCompensationAmount() != null ? domain.getDebtCompensationAmount().getAmount() : null)
                .netAmount(domain.getNetAmount() != null ? domain.getNetAmount().getAmount() : null)
                .periodStart(domain.getPeriodStart())
                .periodEnd(domain.getPeriodEnd())
                .status(domain.getStatus() != null ? domain.getStatus().name() : "CREATED")
                .settlementType(domain.getSettlementType() != null ? domain.getSettlementType().name() : "DAILY")
                .scheduledAt(domain.getScheduledAt())
                .executedAt(domain.getExecutedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : domain.getCreatedAt())
                .build();
    }

    public Settlement toDomain(SettlementEntity entity) {
        return toDomain(entity, List.of());
    }

    public Settlement toDomain(SettlementEntity entity, List<String> dailyBalanceIds) {
        if (entity == null) return null;
        return Settlement.reconstitute(
                entity.getId(),
                entity.getSettlementCode(),
                entity.getProviderId(),
                entity.getProviderType() != null ? ProviderType.valueOf(entity.getProviderType()) : ProviderType.PERSONA_NATURAL,
                new Money(entity.getGrossAmount()),
                new Money(entity.getDebtCompensationAmount()),
                new Money(entity.getNetAmount()),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getStatus() != null ? SettlementStatus.valueOf(entity.getStatus()) : SettlementStatus.CREATED,
                entity.getSettlementType() != null ? SettlementType.valueOf(entity.getSettlementType()) : SettlementType.DAILY,
                entity.getScheduledAt(),
                entity.getExecutedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt(),
                dailyBalanceIds
        );
    }

    // ==========================================
    // PROVIDER SETTLEMENT PROFILE
    // ==========================================
    public ProviderSettlementProfileEntity toEntity(com.findu.transaction.domain.model.settlement.ProviderSettlementProfile domain) {
        if (domain == null) return null;
        return ProviderSettlementProfileEntity.builder()
                .id(domain.getId())
                .providerId(domain.getProviderId())
                .providerType(domain.getProviderType() != null ? domain.getProviderType().name() : null)
                .settlementFrequency(domain.getSettlementFrequency() != null ? domain.getSettlementFrequency().name() : null)
                .settlementDay(domain.getSettlementDay())
                .settlementTime(domain.getSettlementTime())
                .negativeBalanceLimit(domain.getNegativeBalanceLimit() != null ? domain.getNegativeBalanceLimit().getAmount() : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public com.findu.transaction.domain.model.settlement.ProviderSettlementProfile toDomain(ProviderSettlementProfileEntity entity) {
        if (entity == null) return null;
        return com.findu.transaction.domain.model.settlement.ProviderSettlementProfile.reconstitute(
                entity.getId(),
                entity.getProviderId(),
                entity.getProviderType() != null ? ProviderType.valueOf(entity.getProviderType()) : ProviderType.PERSONA_NATURAL,
                entity.getSettlementFrequency() != null ? SettlementType.valueOf(entity.getSettlementFrequency()) : SettlementType.DAILY,
                entity.getSettlementDay(),
                entity.getSettlementTime(),
                new Money(entity.getNegativeBalanceLimit()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // ==========================================
    // EXTRA EXPENSE REQUEST
    // ==========================================
    public ExtraExpenseRequestEntity toEntity(ExtraExpenseRequest domain) {
        if (domain == null) return null;
        return ExtraExpenseRequestEntity.builder()
                .id(domain.getId())
                .serviceRequestId(domain.getServiceRequestId())
                .providerId(domain.getProviderId())
                .description(domain.getReason())
                .requestedAmount(domain.getAmount() != null ? domain.getAmount().getAmount() : null)
                .approvedAmount(domain.getAmount() != null ? domain.getAmount().getAmount() : null)
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .requestedAt(domain.getRequestedAt())
                .approvedAt(domain.getApprovedAt())
                .createdAt(domain.getRequestedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public ExtraExpenseRequest toDomain(ExtraExpenseRequestEntity entity) {
        if (entity == null) return null;
        return ExtraExpenseRequest.reconstitute(
                entity.getId(),
                entity.getServiceRequestId(),
                entity.getProviderId(),
                new Money(entity.getRequestedAmount()),
                entity.getDescription(),
                entity.getStatus() != null ? ExtraExpenseStatus.valueOf(entity.getStatus()) : null,
                entity.getRequestedAt(),
                entity.getApprovedAt(),
                entity.getUpdatedAt()
        );
    }

    // ==========================================
    // PROVIDER DEBT PAYMENT
    // ==========================================
    public ProviderDebtPaymentEntity toEntity(ProviderDebtPayment domain) {
        if (domain == null) return null;
        return ProviderDebtPaymentEntity.builder()
                .id(domain.getId())
                .paymentCode(domain.getPaymentCode())
                .providerId(domain.getProviderId())
                .amount(domain.getAmount() != null ? domain.getAmount().getAmount() : null)
                .paymentMethod(domain.getPaymentMethod() != null ? domain.getPaymentMethod().name() : null)
                .externalReference(domain.getExternalReference())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getCreatedAt())
                .build();
    }

    public ProviderDebtPayment toDomain(ProviderDebtPaymentEntity entity) {
        if (entity == null) return null;
        return ProviderDebtPayment.reconstitute(
                entity.getId(),
                entity.getPaymentCode(),
                entity.getProviderId(),
                new Money(entity.getAmount()),
                entity.getPaymentMethod() != null ? PaymentMethod.valueOf(entity.getPaymentMethod()) : null,
                entity.getExternalReference(),
                entity.getStatus() != null ? PaymentStatus.valueOf(entity.getStatus()) : null,
                entity.getCreatedAt()
        );
    }
}
