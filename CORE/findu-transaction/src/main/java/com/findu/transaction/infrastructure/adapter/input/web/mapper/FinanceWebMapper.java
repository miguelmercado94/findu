package com.findu.transaction.infrastructure.adapter.input.web.mapper;

import com.findu.transaction.application.port.in.finance.command.ConfirmBankConsignmentCommand;
import com.findu.transaction.application.port.in.finance.command.CreateBreBOrderCommand;
import com.findu.transaction.application.port.in.finance.command.ProcessBreBWebhookCommand;
import com.findu.transaction.application.port.in.finance.dto.*;
import com.findu.transaction.domain.model.finance.BreBPaymentOrder;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.ConfirmBankConsignmentRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreateBreBOrderRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.BreBWebhookPayload;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class FinanceWebMapper {

    public WeeklyBalanceResponse toResponse(WeeklyBalanceResult r) {
        if (r == null) return null;

        WeeklyBalanceResponse.NextSettlementDto nextDto = null;
        if (r.getNextSettlementDate() != null) {
            nextDto = WeeklyBalanceResponse.NextSettlementDto.builder()
                    .scheduledDate(r.getNextSettlementDate())
                    .scheduledDateLabel(r.getNextSettlementDateLabel())
                    .status(r.getNextSettlementStatus())
                    .estimatedAmount(r.getNextSettlementEstimatedAmount() != null ? r.getNextSettlementEstimatedAmount().getAmount() : null)
                    .note(r.getNextSettlementNote())
                    .build();
        }

        WeeklyBalanceResponse.WeeklyActivityDto activityDto = WeeklyBalanceResponse.WeeklyActivityDto.builder()
                .completedServicesAmount(r.getCompletedServicesAmount() != null ? r.getCompletedServicesAmount().getAmount() : null)
                .completedServicesCount(r.getCompletedServicesCount())
                .finduCommissionRate(r.getFinduCommissionRate())
                .finduCommissionAmount(r.getFinduCommissionAmount() != null ? r.getFinduCommissionAmount().getAmount() : null)
                .cashPaymentsReceived(r.getCashPaymentsReceived() != null ? r.getCashPaymentsReceived().getAmount() : null)
                .adjustmentsAmount(r.getAdjustmentsAmount() != null ? r.getAdjustmentsAmount().getAmount() : null)
                .totalIncome(r.getTotalIncome() != null ? r.getTotalIncome().getAmount() : null)
                .totalExpenses(r.getTotalExpenses() != null ? r.getTotalExpenses().getAmount() : null)
                .netWeekBalance(r.getNetWeekBalance() != null ? r.getNetWeekBalance().getAmount() : null)
                .build();

        java.math.BigDecimal commissionRatePct = r.getFinduCommissionRate() != null
                ? r.getFinduCommissionRate().multiply(java.math.BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.valueOf(10.0);

        DebtStatusResult debt = r.getDebtSummary();
        java.math.BigDecimal debtLimit = (debt != null && debt.getDebtLimit() != null)
                ? debt.getDebtLimit().getAmount()
                : java.math.BigDecimal.valueOf(125000.0);
        java.math.BigDecimal currentDebt = (debt != null && debt.getCurrentDebt() != null)
                ? debt.getCurrentDebt().getAmount()
                : java.math.BigDecimal.ZERO;
        boolean blocked = debt != null && debt.isBlocked();
        String restStatus = (debt != null && debt.getRestrictionStatus() != null)
                ? debt.getRestrictionStatus().name()
                : "NORMAL";
        String restMessage = (debt != null && debt.getRestrictionMessage() != null)
                ? debt.getRestrictionMessage()
                : "Tu balance está al día. Buena semana.";

        String financialState;
        if (currentDebt.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            financialState = "POSITIVE";
        } else if (blocked || "RESTRICTED".equalsIgnoreCase(restStatus)) {
            financialState = "CRITICAL_NEGATIVE";
        } else {
            financialState = "WARNING_NEGATIVE";
        }

        java.time.LocalDateTime fInicio = r.getFechaInicio() != null ? r.getFechaInicio() : (r.getStartDate() != null ? r.getStartDate().atStartOfDay() : null);
        java.time.LocalDateTime fFin = r.getFechaFin() != null ? r.getFechaFin() : (r.getEndDate() != null ? r.getEndDate().atTime(23, 59, 59) : null);
        java.math.BigDecimal montoBal = r.getProvisionalBalance() != null ? r.getProvisionalBalance().getAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal totServicios = r.getCompletedServicesAmount() != null ? r.getCompletedServicesAmount().getAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal comisionesFindu = r.getFinduCommissionAmount() != null ? r.getFinduCommissionAmount().getAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal pagosEfectivo = r.getCashPaymentsReceived() != null ? r.getCashPaymentsReceived().getAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal ajustes = r.getAdjustmentsAmount() != null ? r.getAdjustmentsAmount().getAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal proxLiqMonto = r.getNextSettlementEstimatedAmount() != null ? r.getNextSettlementEstimatedAmount().getAmount() : java.math.BigDecimal.ZERO;

        return WeeklyBalanceResponse.builder()
                .periodo(r.getPeriodLabel())
                .fechaInicio(fInicio)
                .fechaFin(fFin)
                .montoBalance(montoBal)
                .serviciosCompletados(r.getCompletedServicesCount())
                .serviciosPendientesLiquidar(r.getPendingSettlementCount())
                .pagosEnEfectivoCount(r.getPagosEnEfectivoCount())
                .totalServiciosMonto(totServicios)
                .comisionesFinduMonto(comisionesFindu)
                .comisionPorcentaje(commissionRatePct)
                .pagosEfectivoMonto(pagosEfectivo)
                .ajustesMonto(ajustes)
                .proximaLiquidacionFecha(r.getNextSettlementDateLabel())
                .proximaLiquidacionMonto(proxLiqMonto)
                .proximaLiquidacionEstado(r.getNextSettlementStatus() != null ? r.getNextSettlementStatus() : "PROGRAMADO")
                .limiteDeuda(debtLimit)
                .deudaPendiente(currentDebt)
                .estadoFinanciero(financialState)
                .restrictionStatus(restStatus)
                .isBlocked(blocked)
                .restrictionMessage(restMessage)
                .cutoffNote(r.getCutoffNote() != null ? r.getCutoffNote() : "El corte se realiza el domingo")
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .periodLabel(r.getPeriodLabel())
                .provisionalBalance(montoBal)
                .currency("COP")
                .balanceType(r.getBalanceType())
                .completedServicesCount(r.getCompletedServicesCount())
                .pendingSettlementCount(r.getPendingSettlementCount())
                .nextSettlement(nextDto)
                .weeklyActivity(activityDto)
                .debtSummary(toResponse(r.getDebtSummary()))
                .build();
    }

    public DebtStatusResponse toResponse(DebtStatusResult r) {
        if (r == null) return null;
        return DebtStatusResponse.builder()
                .providerId(r.getProviderId())
                .currentDebt(r.getCurrentDebt() != null ? r.getCurrentDebt().getAmount() : null)
                .debtLimit(r.getDebtLimit() != null ? r.getDebtLimit().getAmount() : null)
                .usedPercentage(r.getUsedPercentage())
                .remainingBeforeBlock(r.getRemainingBeforeBlock() != null ? r.getRemainingBeforeBlock().getAmount() : null)
                .currency("COP")
                .restrictionStatus(r.getRestrictionStatus() != null ? r.getRestrictionStatus().name() : null)
                .isBlocked(r.isBlocked())
                .restrictionMessage(r.getRestrictionMessage())
                .build();
    }

    public FinancialMovementsResponse toResponse(FinancialMovementsResult r) {
        if (r == null) return null;
        return FinancialMovementsResponse.builder()
                .filter(r.getFilter())
                .periodLabel(r.getPeriodLabel())
                .currency("COP")
                .totalIncome(r.getTotalIncome() != null ? r.getTotalIncome().getAmount() : null)
                .totalExpenses(r.getTotalExpenses() != null ? r.getTotalExpenses().getAmount() : null)
                .netBalance(r.getNetBalance() != null ? r.getNetBalance().getAmount() : null)
                .completedServicesCount(r.getCompletedServicesCount())
                .activeDaysCount(r.getActiveDaysCount())
                .movements(r.getMovements() != null ? r.getMovements().stream().map(this::toItemDto).collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }

    private FinancialMovementsResponse.MovementItemDto toItemDto(FinancialMovementItem item) {
        if (item == null) return null;
        return FinancialMovementsResponse.MovementItemDto.builder()
                .id(item.getId())
                .serviceRequestId(item.getServiceRequestId())
                .serviceCode(item.getServiceCode())
                .serviceName(item.getServiceName())
                .specializationName(item.getSpecializationName())
                .title(item.getTitle())
                .subtitle(item.getSubtitle())
                .date(item.getDate())
                .dateLabel(item.getDateLabel())
                .timeLabel(item.getTimeLabel())
                .amount(item.getAmount() != null ? item.getAmount().getAmount() : null)
                .currency("COP")
                .isIncome(item.isIncome())
                .type(item.getType())
                .status(item.getStatus())
                .reference(item.getReference())
                .build();
    }

    public BankConsignmentInfoResponse toResponse(BankConsignmentInfoResult r) {
        if (r == null) return null;
        return BankConsignmentInfoResponse.builder()
                .bankName(r.getBankName())
                .accountType(r.getAccountType())
                .accountNumber(r.getAccountNumber())
                .maskedAccountNumber(r.getMaskedAccountNumber())
                .accountHolder(r.getAccountHolder())
                .nit(r.getNit())
                .paymentReference(r.getPaymentReference())
                .instructions(r.getInstructions())
                .build();
    }

    public BreBOrderResponse toResponse(BreBPaymentOrder order) {
        if (order == null) return null;
        Instant now = Instant.now();
        long remaining = Math.max(0, Duration.between(now, order.getExpiresAt()).getSeconds());

        return BreBOrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .providerId(order.getProviderId())
                .amount(order.getAmount() != null ? order.getAmount().getAmount() : null)
                .currency("COP")
                .breBKey(order.getBreBKey())
                .qrPayload(order.getQrPayload())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .expiresAt(order.getExpiresAt())
                .secondsRemaining(remaining)
                .confirmedAt(order.getConfirmedAt())
                .externalTransactionId(order.getExternalTransactionId())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public CreateBreBOrderCommand toCommand(Long providerId, CreateBreBOrderRequest req) {
        if (req == null) return null;
        return CreateBreBOrderCommand.builder()
                .providerId(providerId)
                .amount(Money.of(req.getAmount()))
                .username(req.getUsername())
                .build();
    }

    public ProcessBreBWebhookCommand toCommand(BreBWebhookPayload payload, String signature, String rawJson) {
        if (payload == null) return null;
        return ProcessBreBWebhookCommand.builder()
                .orderId(payload.getOrderId())
                .orderCode(payload.getOrderCode())
                .transactionId(payload.getTransactionId())
                .amount(payload.getAmount() != null ? Money.of(payload.getAmount()) : null)
                .status(payload.getStatus())
                .signature(signature)
                .rawPayload(rawJson)
                .build();
    }

    public ConfirmBankConsignmentCommand toCommand(ConfirmBankConsignmentRequest req) {
        if (req == null) return null;
        return ConfirmBankConsignmentCommand.builder()
                .providerId(req.getProviderId())
                .amount(Money.of(req.getAmount()))
                .reference(req.getReference())
                .bankName(req.getBankName())
                .confirmedBy(req.getConfirmedBy())
                .notes(req.getNotes())
                .build();
    }
}
