package com.findu.transaction.infrastructure.adapter.input.web.mapper;

import com.findu.transaction.application.port.in.command.*;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.debt.ProviderDebtPayment;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.domain.model.refund.Refund;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.*;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TransactionWebMapper {

    // Commands
    public CreateServiceTransactionCommand toCommand(CreateTransactionRequest req) {
        if (req == null) return null;
        return CreateServiceTransactionCommand.builder()
                .serviceRequestId(req.getServiceRequestId())
                .providerId(req.getProviderId())
                .customerId(req.getCustomerId())
                .serviceAmount(new Money(req.getServiceAmount()))
                .commissionRate(req.getCommissionRate())
                .tipAmount(req.getTipAmount() != null ? new Money(req.getTipAmount()) : Money.ZERO)
                .extraAmount(req.getExtraAmount() != null ? new Money(req.getExtraAmount()) : Money.ZERO)
                .paymentMethod(req.getPaymentMethod())
                .build();
    }

    public RequestExtraExpenseCommand toCommand(RequestExtraExpenseRequest req) {
        if (req == null) return null;
        return RequestExtraExpenseCommand.builder()
                .serviceRequestId(req.getServiceRequestId())
                .providerId(req.getProviderId())
                .amount(new Money(req.getAmount()))
                .reason(req.getReason())
                .build();
    }

    public CreatePayoutCommand toCommand(CreatePayoutRequest req) {
        if (req == null) return null;
        return CreatePayoutCommand.builder()
                .providerId(req.getProviderId())
                .settlementId(req.getSettlementId())
                .amount(req.getAmount() != null ? new Money(req.getAmount()) : null)
                .build();
    }

    public CreateRefundCommand toCommand(CreateRefundRequest req) {
        if (req == null) return null;
        return CreateRefundCommand.builder()
                .transactionId(req.getTransactionId())
                .customerId(req.getCustomerId())
                .amount(new Money(req.getAmount()))
                .reason(req.getReason())
                .build();
    }

    public PayProviderDebtCommand toCommand(PayDebtRequest req) {
        if (req == null) return null;
        return PayProviderDebtCommand.builder()
                .providerId(req.getProviderId())
                .amount(new Money(req.getAmount()))
                .paymentMethod(req.getPaymentMethod())
                .externalReference(req.getExternalReference())
                .build();
    }

    public RegisterPaymentCommand toCommand(String transactionId, CreatePaymentRequest req) {
        if (req == null) return null;
        return RegisterPaymentCommand.builder()
                .transactionId(transactionId)
                .customerId(req.getCustomerId())
                .amount(req.getAmount() != null ? new Money(req.getAmount()) : null)
                .paymentMethod(req.getPaymentMethod())
                .reference(req.getReference())
                .build();
    }

    public CreateSettlementCommand toCommand(CreateSettlementRequest req) {
        if (req == null) return null;
        return CreateSettlementCommand.builder()
                .providerId(req.getProviderId())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();
    }

    // Responses
    public ServiceTransactionResponse toResponse(ServiceTransaction domain) {
        if (domain == null) return null;
        ServiceTransactionResponse res = new ServiceTransactionResponse();
        res.setId(domain.getId());
        res.setTransactionCode(domain.getTransactionCode().getValue());
        res.setServiceRequestId(domain.getServiceRequestId());
        res.setProviderId(domain.getProviderId());
        res.setCustomerId(domain.getCustomerId());
        res.setServiceAmount(domain.getServiceAmount().getAmount());
        res.setCommissionRate(domain.getCommissionRate());
        res.setCommissionAmount(domain.getCommissionAmount().getAmount());
        res.setTipAmount(domain.getTipAmount().getAmount());
        res.setExtraAmount(domain.getExtraAmount().getAmount());
        res.setTotalCustomerAmount(domain.getTotalCustomerAmount().getAmount());
        res.setProviderAmount(domain.getProviderAmount().getAmount());
        res.setCurrency(domain.getServiceAmount().getCurrency().getCurrencyCode());
        res.setPaymentMethod(domain.getPaymentMethod());
        res.setPaymentStatus(domain.getPaymentStatus());
        res.setTransactionStatus(domain.getTransactionStatus());
        res.setServiceCompletedAt(domain.getServiceCompletedAt());
        res.setCreatedAt(domain.getCreatedAt());
        res.setUpdatedAt(domain.getUpdatedAt());
        return res;
    }

    public ProviderAccountResponse toResponse(ProviderAccount domain) {
        if (domain == null) return null;
        ProviderAccountResponse res = new ProviderAccountResponse();
        res.setId(domain.getId());
        res.setProviderId(domain.getProviderId());
        res.setProviderType(domain.getProviderType());
        res.setPayableBalance(domain.getPayableBalance().getAmount());
        res.setReceivableBalance(domain.getReceivableBalance().getAmount());
        res.setDebtLimit(domain.getDebtLimit().getAmount());
        res.setCurrency(domain.getPayableBalance().getCurrency().getCurrencyCode());
        res.setStatus(domain.getStatus());
        res.setCreatedAt(domain.getCreatedAt());
        res.setUpdatedAt(domain.getUpdatedAt());

        List<ProviderLedgerEntryResponse> entries = domain.getLedgerEntries().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        res.setLedgerEntries(entries);
        return res;
    }

    public ProviderLedgerEntryResponse toResponse(ProviderLedgerEntry domain) {
        if (domain == null) return null;
        ProviderLedgerEntryResponse res = new ProviderLedgerEntryResponse();
        res.setEntryId(domain.getEntryId());
        res.setProviderId(domain.getProviderId());
        res.setType(domain.getType());
        res.setAmount(domain.getAmount().getAmount());
        res.setCurrency(domain.getAmount().getCurrency().getCurrencyCode());
        res.setDirection(domain.getDirection());
        res.setReferenceId(domain.getReferenceId());
        res.setDescription(domain.getDescription());
        res.setCreatedAt(domain.getCreatedAt());
        return res;
    }

    public ExtraExpenseResponse toResponse(ExtraExpenseRequest domain) {
        if (domain == null) return null;
        ExtraExpenseResponse res = new ExtraExpenseResponse();
        res.setId(domain.getId());
        res.setServiceRequestId(domain.getServiceRequestId());
        res.setProviderId(domain.getProviderId());
        res.setAmount(domain.getAmount().getAmount());
        res.setCurrency(domain.getAmount().getCurrency().getCurrencyCode());
        res.setReason(domain.getReason());
        res.setStatus(domain.getStatus());
        res.setRequestedAt(domain.getRequestedAt());
        res.setApprovedAt(domain.getApprovedAt());
        res.setUpdatedAt(domain.getUpdatedAt());
        return res;
    }

    public PayoutResponse toResponse(Payout domain) {
        if (domain == null) return null;
        PayoutResponse res = new PayoutResponse();
        res.setId(domain.getId());
        res.setPayoutCode(domain.getPayoutCode());
        res.setSettlementId(domain.getSettlementId());
        res.setProviderId(domain.getProviderId());
        res.setAmount(domain.getAmount().getAmount());
        res.setCurrency(domain.getAmount().getCurrency().getCurrencyCode());
        res.setStatus(domain.getStatus());
        res.setExternalReference(domain.getExternalReference());
        res.setFailureReason(domain.getFailureReason());
        res.setCreatedAt(domain.getCreatedAt());
        res.setProcessedAt(domain.getProcessedAt());
        res.setCompletedAt(domain.getCompletedAt());
        return res;
    }

    public RefundResponse toResponse(Refund domain) {
        if (domain == null) return null;
        RefundResponse res = new RefundResponse();
        res.setId(domain.getId());
        res.setRefundCode(domain.getRefundCode());
        res.setTransactionId(domain.getTransactionId());
        res.setCustomerId(domain.getCustomerId());
        res.setAmount(domain.getAmount().getAmount());
        res.setCurrency(domain.getAmount().getCurrency().getCurrencyCode());
        res.setReason(domain.getReason());
        res.setStatus(domain.getStatus());
        res.setExternalReference(domain.getExternalReference());
        res.setFailureReason(domain.getFailureReason());
        res.setCreatedAt(domain.getCreatedAt());
        res.setProcessedAt(domain.getProcessedAt());
        res.setCompletedAt(domain.getCompletedAt());
        return res;
    }

    public DailyBalanceResponse toResponse(DailyBalance domain) {
        if (domain == null) return null;
        DailyBalanceResponse res = new DailyBalanceResponse();
        res.setId(domain.getId());
        res.setProviderId(domain.getProviderId());
        res.setBalanceDate(domain.getBalanceDate());
        res.setOpeningBalance(domain.getOpeningBalance().getAmount());
        res.setTotalEarnings(domain.getTotalEarnings().getAmount());
        res.setTotalCommissions(domain.getTotalCommissions().getAmount());
        res.setTotalTips(domain.getTotalTips().getAmount());
        res.setTotalExtras(domain.getTotalExtras().getAmount());
        res.setCashPayments(domain.getCashPayments().getAmount());
        res.setDigitalPayments(domain.getDigitalPayments().getAmount());
        res.setProviderDebt(domain.getProviderDebt().getAmount());
        res.setPendingPayout(domain.getPendingPayout().getAmount());
        res.setClosingBalance(domain.getClosingBalance().getAmount());
        res.setCurrency(domain.getClosingBalance().getCurrency().getCurrencyCode());
        res.setCreatedAt(domain.getCreatedAt());
        return res;
    }

    public ProviderDebtPaymentResponse toResponse(ProviderDebtPayment domain) {
        if (domain == null) return null;
        ProviderDebtPaymentResponse res = new ProviderDebtPaymentResponse();
        res.setId(domain.getId());
        res.setPaymentCode(domain.getPaymentCode());
        res.setProviderId(domain.getProviderId());
        res.setAmount(domain.getAmount().getAmount());
        res.setCurrency(domain.getAmount().getCurrency().getCurrencyCode());
        res.setPaymentMethod(domain.getPaymentMethod());
        res.setExternalReference(domain.getExternalReference());
        res.setStatus(domain.getStatus());
        res.setCreatedAt(domain.getCreatedAt());
        return res;
    }

    public SettlementResponse toResponse(Settlement domain) {
        if (domain == null) return null;
        SettlementResponse res = new SettlementResponse();
        res.setId(domain.getId());
        res.setSettlementCode(domain.getSettlementCode());
        res.setProviderId(domain.getProviderId());
        res.setProviderType(domain.getProviderType());
        res.setGrossAmount(domain.getGrossAmount() != null ? domain.getGrossAmount().getAmount() : null);
        res.setDebtCompensationAmount(domain.getDebtCompensationAmount() != null ? domain.getDebtCompensationAmount().getAmount() : null);
        res.setNetAmount(domain.getNetAmount() != null ? domain.getNetAmount().getAmount() : null);
        res.setCurrency(domain.getGrossAmount() != null ? domain.getGrossAmount().getCurrency().getCurrencyCode() : "COP");
        res.setStatus(domain.getStatus());
        res.setSettlementType(domain.getSettlementType());
        res.setPeriodStart(domain.getPeriodStart());
        res.setPeriodEnd(domain.getPeriodEnd());
        res.setCreatedAt(domain.getCreatedAt());
        res.setDailyBalanceIds(domain.getDailyBalanceIds());
        return res;
    }

    public List<ServiceTransactionResponse> toTransactionResponseList(List<ServiceTransaction> list) {
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<PayoutResponse> toPayoutResponseList(List<Payout> list) {
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<RefundResponse> toRefundResponseList(List<Refund> list) {
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<DailyBalanceResponse> toDailyBalanceResponseList(List<DailyBalance> list) {
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<SettlementResponse> toSettlementResponseList(List<Settlement> list) {
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
