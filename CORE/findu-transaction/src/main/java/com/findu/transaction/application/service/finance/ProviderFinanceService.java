package com.findu.transaction.application.service.finance;

import com.findu.transaction.application.port.in.finance.*;
import com.findu.transaction.application.port.in.finance.command.ConfirmBankConsignmentCommand;
import com.findu.transaction.application.port.in.finance.command.CreateBreBOrderCommand;
import com.findu.transaction.application.port.in.finance.command.ProcessBreBWebhookCommand;
import com.findu.transaction.application.port.in.finance.dto.*;
import com.findu.transaction.application.port.out.gateway.BreBPaymentGatewayPort;
import com.findu.transaction.application.port.out.persistence.*;
import com.findu.transaction.domain.enums.*;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.debt.ProviderDebtPayment;
import com.findu.transaction.domain.model.finance.BankConsignmentReport;
import com.findu.transaction.domain.model.finance.BreBPaymentOrder;
import com.findu.transaction.domain.model.settlement.ProviderSettlementProfile;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderFinanceService implements
        GetWeeklyBalanceUseCase,
        GetDebtStatusUseCase,
        GetFinancialMovementsUseCase,
        GetBankConsignmentInfoUseCase,
        CreateBreBOrderUseCase,
        GetBreBOrderUseCase,
        ProcessBreBWebhookUseCase,
        ConfirmBankConsignmentUseCase {

    private final ProviderAccountRepository providerAccountRepository;
    private final ProviderSettlementProfileRepository profileRepository;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final ServiceTransactionRepository serviceTransactionRepository;
    private final ProviderDebtPaymentRepository providerDebtPaymentRepository;
    private final BreBPaymentOrderRepository breBOrderRepository;
    private final BankConsignmentReportRepository bankConsignmentRepository;
    private final BreBPaymentGatewayPort breBGatewayPort;

    @Value("${findu.finance.bank.name:Bancolombia}")
    private String companyBankName;

    @Value("${findu.finance.bank.account-type:Ahorros}")
    private String companyAccountType;

    @Value("${findu.finance.bank.account-number:031-987654-32}")
    private String companyAccountNumber;

    @Value("${findu.finance.bank.account-holder:FindU S.A.S.}")
    private String companyAccountHolder;

    @Value("${findu.finance.bank.nit:901.765.432-1}")
    private String companyNit;

    @Value("${findu.finance.default-debt-limit:125000.00}")
    private BigDecimal defaultDebtLimit;

    private static final String DEFAULT_TIMEZONE = "America/Bogota";
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.10");

    @Override
    @Transactional(readOnly = true)
    public WeeklyBalanceResult getWeeklyBalance(Long providerId, LocalDate targetDate, String username) {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId es obligatorio");
        }

        ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
        LocalDate refDate = targetDate != null ? targetDate : LocalDate.now(zoneId);

        LocalDate startDate = refDate.with(DayOfWeek.MONDAY);
        LocalDate endDate = refDate.with(DayOfWeek.SUNDAY);

        String periodLabel = formatWeeklyPeriodLabel(startDate, endDate);

        // Próxima liquidación: Martes siguiente al domingo de corte
        LocalDate nextSettlementDate = endDate.plusDays(2);
        String nextSettlementDateLabel = formatSettlementDateLabel(nextSettlementDate);

        // Obtener la cuenta del proveedor
        ProviderAccount account = providerAccountRepository.findByProviderId(providerId)
                .orElseGet(() -> ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL));

        // Obtener balances diarios de la semana
        List<DailyBalance> weekBalances = dailyBalanceRepository.findByProviderIdAndBalanceDateBetween(
                providerId, startDate, endDate);

        // Transacciones de servicio de la semana
        List<ServiceTransaction> allTx = serviceTransactionRepository.findByProviderId(providerId);
        List<ServiceTransaction> weekTx = allTx.stream()
                .filter(tx -> tx.getCreatedAt() != null)
                .filter(tx -> {
                    LocalDate txDate = tx.getCreatedAt().atZone(zoneId).toLocalDate();
                    return !txDate.isBefore(startDate) && !txDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        BigDecimal totalServiceGross = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalCash = BigDecimal.ZERO;
        int completedCount = 0;

        for (ServiceTransaction tx : weekTx) {
            if (tx.getTransactionStatus() != TransactionStatus.CANCELLED) {
                completedCount++;
                totalServiceGross = totalServiceGross.add(tx.getServiceAmount().getAmount());
                totalCommission = totalCommission.add(tx.getCommissionAmount().getAmount());
                if (tx.getPaymentMethod() == PaymentMethod.CASH) {
                    totalCash = totalCash.add(tx.getTotalCustomerAmount().getAmount());
                }
            }
        }

        // Si no hay transacciones en el repo pero sí DailyBalances, usar DailyBalances
        if (completedCount == 0 && !weekBalances.isEmpty()) {
            for (DailyBalance db : weekBalances) {
                totalServiceGross = totalServiceGross.add(db.getTotalEarnings().getAmount());
                totalCommission = totalCommission.add(db.getTotalCommissions().getAmount());
                totalCash = totalCash.add(db.getCashPayments().getAmount());
            }
        }

        Money completedServicesMoney = Money.of(totalServiceGross);
        Money commissionMoney = Money.of(totalCommission.negate()); // Negativo en UI
        Money cashMoney = Money.of(totalCash.negate());             // Negativo en UI
        Money adjustmentsMoney = Money.ZERO;

        Money totalIncome = completedServicesMoney;
        Money totalExpenses = Money.of(totalCommission.add(totalCash).negate());
        Money netWeekBalance = completedServicesMoney.subtract(Money.of(totalCommission)).subtract(Money.of(totalCash));

        // Resumen de deuda y límites
        DebtStatusResult debtSummary = getDebtStatus(providerId);

        // Determinación del tipo de balance para la UI
        String balanceType;
        Money provisionalBalance;
        Money nextSettlementEstimated;

        if (debtSummary.getCurrentDebt().isPositive()) {
            // Saldo pendiente / negativo
            provisionalBalance = Money.of(debtSummary.getCurrentDebt().getAmount().negate());
            balanceType = debtSummary.isBlocked() ? "RESTRICTED" : (debtSummary.getRestrictionStatus() == FinanceRestrictionStatus.WARNING ? "NEGATIVE_WARNING" : "PENDING_DEBT");
            nextSettlementEstimated = Money.ZERO;
        } else {
            // Saldo positivo / a favor
            provisionalBalance = netWeekBalance.isPositive() ? netWeekBalance : account.getPayableBalance();
            balanceType = "PROVISIONAL";
            nextSettlementEstimated = provisionalBalance;
        }

        return WeeklyBalanceResult.builder()
                .startDate(startDate)
                .endDate(endDate)
                .periodLabel(periodLabel)
                .provisionalBalance(provisionalBalance)
                .balanceType(balanceType)
                .completedServicesCount(completedCount)
                .pendingSettlementCount(completedCount > 0 ? Math.max(1, completedCount / 2) : 0)
                .cutoffNote("El corte se realiza el domingo")
                .nextSettlementDate(nextSettlementDate)
                .nextSettlementDateLabel(nextSettlementDateLabel)
                .nextSettlementStatus("PROGRAMADO")
                .nextSettlementEstimatedAmount(nextSettlementEstimated)
                .nextSettlementNote("El saldo a favor se liquida después del corte semanal.")
                .completedServicesAmount(completedServicesMoney)
                .finduCommissionRate(DEFAULT_COMMISSION_RATE)
                .finduCommissionAmount(commissionMoney)
                .cashPaymentsReceived(cashMoney)
                .adjustmentsAmount(adjustmentsMoney)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netWeekBalance(netWeekBalance)
                .debtSummary(debtSummary)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DebtStatusResult getDebtStatus(Long providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId es obligatorio");
        }

        ProviderAccount account = providerAccountRepository.findByProviderId(providerId)
                .orElseGet(() -> ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL));

        ProviderSettlementProfile profile = profileRepository.findByProviderId(providerId)
                .orElse(null);

        Money debtLimit = profile != null && profile.getNegativeBalanceLimit() != null
                ? profile.getNegativeBalanceLimit()
                : (account.getDebtLimit() != null && account.getDebtLimit().isPositive()
                    ? account.getDebtLimit()
                    : Money.of(defaultDebtLimit));

        // Deuda real neta: receivableBalance (deuda por efectivo) - payableBalance (saldo a favor de FindU)
        Money receivable = account.getReceivableBalance();
        Money payable = account.getPayableBalance();
        Money currentDebt = receivable.isGreaterThan(payable)
                ? receivable.subtract(payable)
                : Money.ZERO;

        double usedPercentage = 0.0;
        Money remainingBeforeBlock = debtLimit;
        FinanceRestrictionStatus restrictionStatus = FinanceRestrictionStatus.NORMAL;
        boolean isBlocked = false;
        String restrictionMessage = "Tu balance está al día. Buena semana.";

        if (currentDebt.isPositive() && debtLimit.isPositive()) {
            BigDecimal debtAmt = currentDebt.getAmount();
            BigDecimal limitAmt = debtLimit.getAmount();

            usedPercentage = debtAmt.divide(limitAmt, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();

            if (debtAmt.compareTo(limitAmt) >= 0) {
                restrictionStatus = FinanceRestrictionStatus.RESTRICTED;
                isBlocked = true;
                remainingBeforeBlock = Money.ZERO;
                restrictionMessage = "Has superado el límite de deuda. Tu cuenta será bloqueada hasta que realices un pago.";
            } else if (usedPercentage >= 80.0) {
                restrictionStatus = FinanceRestrictionStatus.WARNING;
                remainingBeforeBlock = debtLimit.subtract(currentDebt);
                restrictionMessage = "Estás en límite de deuda. Puedes seguir operando, pero te recomendamos consignar pronto.";
            } else {
                restrictionStatus = FinanceRestrictionStatus.NORMAL;
                remainingBeforeBlock = debtLimit.subtract(currentDebt);
                restrictionMessage = "Tienes un saldo negativo. Paga a tiempo para evitar restricciones en la plataforma.";
            }
        }

        return DebtStatusResult.builder()
                .providerId(providerId)
                .currentDebt(currentDebt)
                .debtLimit(debtLimit)
                .usedPercentage(usedPercentage)
                .remainingBeforeBlock(remainingBeforeBlock)
                .restrictionStatus(restrictionStatus)
                .isBlocked(isBlocked)
                .restrictionMessage(restrictionMessage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialMovementsResult getMovements(Long providerId, String filter) {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId es obligatorio");
        }

        String activeFilter = filter != null ? filter.trim().toUpperCase() : "THIS_WEEK";
        ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
        LocalDate today = LocalDate.now(zoneId);

        Instant threshold;
        String periodLabel;

        if ("LAST_30_DAYS".equals(activeFilter)) {
            threshold = today.minusDays(30).atStartOfDay(zoneId).toInstant();
            periodLabel = "Últimos 30 días";
        } else if ("ALL".equals(activeFilter)) {
            threshold = Instant.EPOCH;
            periodLabel = "Historial completo";
        } else {
            // THIS_WEEK
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            threshold = monday.atStartOfDay(zoneId).toInstant();
            LocalDate sunday = today.with(DayOfWeek.SUNDAY);
            periodLabel = formatWeeklyPeriodLabel(monday, sunday);
        }

        ProviderAccount account = providerAccountRepository.findByProviderId(providerId)
                .orElseGet(() -> ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL));

        List<ProviderLedgerEntry> entries = account.getLedgerEntries().stream()
                .filter(e -> e.getCreatedAt() != null && !e.getCreatedAt().isBefore(threshold))
                .sorted(Comparator.comparing(ProviderLedgerEntry::getCreatedAt).reversed())
                .collect(Collectors.toList());

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        List<FinancialMovementItem> items = new ArrayList<>();

        for (ProviderLedgerEntry e : entries) {
            boolean isCredit = e.getDirection() == LedgerDirection.CREDIT;
            boolean isIncome = isCredit;

            String title;
            String subtitle = e.getDescription();
            String status = "COMPLETADO";

            switch (e.getType()) {
                case SERVICE_EARNING:
                    title = "Ganancia de servicio";
                    isIncome = true;
                    income = income.add(e.getAmount().getAmount());
                    break;
                case PLATFORM_COMMISSION:
                    title = "Comisión FindU (10%)";
                    subtitle = "Sobre tus ganancias";
                    isIncome = false;
                    expenses = expenses.add(e.getAmount().getAmount());
                    break;
                case CASH_PAYMENT_DEBT:
                    title = "Pago en efectivo recibido";
                    subtitle = "De clientes";
                    isIncome = false;
                    expenses = expenses.add(e.getAmount().getAmount());
                    break;
                case DEBT_PAYMENT:
                    title = e.getReferenceId().startsWith("BREB") ? "Pago por Bre-B" : "Consignación bancaria";
                    subtitle = "Abono a tu deuda";
                    isIncome = true;
                    status = "RECIBIDO";
                    income = income.add(e.getAmount().getAmount());
                    break;
                case PAYOUT:
                    title = "Liquidación semanal";
                    subtitle = "Transferencia a cuenta";
                    isIncome = true;
                    status = "PROGRAMADO";
                    break;
                case COMPENSATION_ADJUSTMENT:
                    title = "Ajuste de servicio";
                    subtitle = "Compensación de balance";
                    isIncome = isCredit;
                    break;
                default:
                    title = e.getType().name();
                    break;
            }

            items.add(FinancialMovementItem.builder()
                    .id(e.getEntryId())
                    .title(title)
                    .subtitle(subtitle)
                    .date(e.getCreatedAt())
                    .dateLabel(formatMovementDate(e.getCreatedAt()))
                    .amount(isIncome ? e.getAmount() : Money.of(e.getAmount().getAmount().negate()))
                    .isIncome(isIncome)
                    .type(e.getType().name())
                    .status(status)
                    .reference(e.getReferenceId())
                    .build());
        }

        Money totalIncomeMoney = Money.of(income);
        Money totalExpensesMoney = Money.of(expenses.negate());
        Money netBalanceMoney = totalIncomeMoney.subtract(Money.of(expenses));

        return FinancialMovementsResult.builder()
                .filter(activeFilter)
                .periodLabel(periodLabel)
                .totalIncome(totalIncomeMoney)
                .totalExpenses(totalExpensesMoney)
                .netBalance(netBalanceMoney)
                .movements(items)
                .build();
    }

    @Override
    public BankConsignmentInfoResult getBankConsignmentInfo(Long providerId, String username) {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId es obligatorio");
        }

        String cleanUsername = username != null && !username.isBlank()
                ? username.trim().toLowerCase().replaceAll("[^a-z0-9]", "")
                : "proveedor" + providerId;

        // Formato interno: nameuser-{serial-entidad-bancaria} (ej: miguelm-BANCOLOMBIA-1234)
        String paymentReference = String.format("%s-BANCOLOMBIA-%04d", cleanUsername, (providerId % 10000));
        String maskedAccount = companyAccountNumber.length() > 4
                ? "********" + companyAccountNumber.substring(companyAccountNumber.length() - 4)
                : companyAccountNumber;

        return BankConsignmentInfoResult.builder()
                .bankName(companyBankName)
                .accountType(companyAccountType)
                .accountNumber(companyAccountNumber)
                .maskedAccountNumber(maskedAccount)
                .accountHolder(companyAccountHolder)
                .nit(companyNit)
                .paymentReference(paymentReference)
                .instructions("Realiza la consignación desde tu banco usando los datos de la cuenta empresarial. El pago se conciliará con tu referencia.")
                .build();
    }

    @Override
    @Transactional
    public BreBPaymentOrder createOrder(CreateBreBOrderCommand command) {
        if (command == null || command.getProviderId() == null || command.getAmount() == null) {
            throw new IllegalArgumentException("El comando requiere providerId y monto");
        }
        if (!command.getAmount().isPositive()) {
            throw new IllegalArgumentException("El monto a abonar debe ser mayor a cero");
        }

        Long providerId = command.getProviderId();
        DebtStatusResult debt = getDebtStatus(providerId);

        // Validación: no permitir que el proveedor abone un monto superior a su deuda pendiente
        if (debt.getCurrentDebt().isPositive() && command.getAmount().isGreaterThan(debt.getCurrentDebt())) {
            throw new IllegalArgumentException(String.format(
                    "El monto a pagar (%s) no puede superar la deuda pendiente actual (%s)",
                    command.getAmount().getAmount(), debt.getCurrentDebt().getAmount()));
        }

        String breBKey = breBGatewayPort.generatePersonalizedKey(providerId, command.getUsername());
        String orderCode = "BREB-ORD-" + Instant.now().toEpochMilli();
        String qrPayload = breBGatewayPort.generateDynamicQrPayload(breBKey, command.getAmount(), orderCode, "Abono de deuda FindU");

        BreBPaymentOrder order = BreBPaymentOrder.create(
                providerId,
                command.getAmount(),
                breBKey,
                qrPayload,
                Duration.ofMinutes(10)
        );

        BreBPaymentOrder saved = breBOrderRepository.save(order);
        log.info("Orden Bre-B creada: orderCode={}, providerId={}, amount={}, key={}",
                saved.getOrderCode(), providerId, saved.getAmount().getAmount(), saved.getBreBKey());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BreBPaymentOrder> getOrderById(String orderId) {
        if (orderId == null || orderId.isBlank()) return Optional.empty();
        Optional<BreBPaymentOrder> opt = breBOrderRepository.findById(orderId);
        opt.ifPresent(BreBPaymentOrder::checkAndMarkExpired);
        return opt;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BreBPaymentOrder> getOrderByCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) return Optional.empty();
        Optional<BreBPaymentOrder> opt = breBOrderRepository.findByOrderCode(orderCode);
        opt.ifPresent(BreBPaymentOrder::checkAndMarkExpired);
        return opt;
    }

    @Override
    @Transactional
    public BreBPaymentOrder processWebhook(ProcessBreBWebhookCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El payload del webhook es obligatorio");
        }

        // 1. Validar autenticidad de la firma / token secreto
        if (!breBGatewayPort.validateWebhookSignature(command.getSignature(), command.getRawPayload())) {
            log.warn("Firma inválida en webhook Bre-B: signature={}", command.getSignature());
            throw new SecurityException("Firma de webhook Bre-B no autorizada");
        }

        // 2. Buscar la orden
        BreBPaymentOrder order = null;
        if (command.getOrderId() != null && !command.getOrderId().isBlank()) {
            order = breBOrderRepository.findById(command.getOrderId()).orElse(null);
        }
        if (order == null && command.getOrderCode() != null && !command.getOrderCode().isBlank()) {
            order = breBOrderRepository.findByOrderCode(command.getOrderCode()).orElse(null);
        }
        if (order == null) {
            log.warn("Orden Bre-B no encontrada para webhook: orderId={}, orderCode={}", command.getOrderId(), command.getOrderCode());
            throw new NoSuchElementException("Orden de pago Bre-B no encontrada");
        }

        // 3. Idempotencia: si ya fue confirmada, retornar sin re-aplicar
        if (order.getStatus() == BreBOrderStatus.CONFIRMED) {
            log.info("Webhook Bre-B ya procesado previamente (Idempotente): orderCode={}", order.getOrderCode());
            return order;
        }

        // 4. Confirmar orden
        String txId = command.getTransactionId() != null ? command.getTransactionId() : "TX-BREB-" + UUID.randomUUID().toString().substring(0, 8);
        order.confirm(txId);
        BreBPaymentOrder savedOrder = breBOrderRepository.save(order);

        // 5. Aplicar abono contable a la cuenta del proveedor (Ledger y reducción de deuda)
        Long providerId = savedOrder.getProviderId();
        ProviderAccount account = providerAccountRepository.findByProviderId(providerId)
                .orElseGet(() -> ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL));

        account.recordDebtPayment(savedOrder.getAmount(), savedOrder.getOrderCode());
        providerAccountRepository.save(account);

        // 6. Registrar en provider_debt_payments para trazabilidad de pagos de deuda
        ProviderDebtPayment debtPayment = ProviderDebtPayment.create(
                providerId,
                savedOrder.getAmount(),
                PaymentMethod.BRE_B,
                savedOrder.getOrderCode()
        );
        providerDebtPaymentRepository.save(debtPayment);

        log.info("Pago Bre-B confirmado exitosamente: providerId={}, amount={}, newReceivable={}, newStatus={}",
                providerId, savedOrder.getAmount().getAmount(), account.getReceivableBalance().getAmount(), account.getStatus());

        return savedOrder;
    }

    @Override
    @Transactional
    public BankConsignmentReport confirmConsignment(ConfirmBankConsignmentCommand command) {
        if (command == null || command.getProviderId() == null || command.getAmount() == null) {
            throw new IllegalArgumentException("El comando de consignación requiere providerId y monto");
        }

        Long providerId = command.getProviderId();
        BankConsignmentReport report = bankConsignmentRepository.findByReference(command.getReference())
                .orElseGet(() -> BankConsignmentReport.create(
                        providerId,
                        command.getAmount(),
                        command.getReference(),
                        command.getBankName(),
                        command.getNotes()
                ));

        if (report.getStatus() == PaymentStatus.COMPLETED) {
            return report; // Idempotente
        }

        report.confirm(command.getConfirmedBy());
        BankConsignmentReport savedReport = bankConsignmentRepository.save(report);

        // Aplicar a la cuenta y ledger
        ProviderAccount account = providerAccountRepository.findByProviderId(providerId)
                .orElseGet(() -> ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL));

        account.recordDebtPayment(command.getAmount(), command.getReference());
        providerAccountRepository.save(account);

        // Registrar en pagos de deuda
        ProviderDebtPayment debtPayment = ProviderDebtPayment.create(
                providerId,
                command.getAmount(),
                PaymentMethod.TRANSFER,
                command.getReference()
        );
        providerDebtPaymentRepository.save(debtPayment);

        log.info("Consignación bancaria confirmada: providerId={}, amount={}, ref={}",
                providerId, command.getAmount().getAmount(), command.getReference());

        return savedReport;
    }

    private String formatWeeklyPeriodLabel(LocalDate start, LocalDate end) {
        DateTimeFormatter dayMonth = DateTimeFormatter.ofPattern("d MMM", new Locale("es", "CO"));
        String startStr = start.format(dayMonth);
        String endStr = end.format(dayMonth);
        return String.format("Lun %s — Dom %s", startStr, endStr);
    }

    private String formatSettlementDateLabel(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es", "CO"));
        String str = date.format(formatter);
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String formatMovementDate(Instant instant) {
        if (instant == null) return "";
        ZonedDateTime zdt = instant.atZone(ZoneId.of(DEFAULT_TIMEZONE));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMM", new Locale("es", "CO"));
        String str = zdt.format(formatter);
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
