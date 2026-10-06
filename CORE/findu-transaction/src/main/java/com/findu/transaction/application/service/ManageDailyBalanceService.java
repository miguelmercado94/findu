package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.CalculateDailyBalanceUseCase;
import com.findu.transaction.application.port.in.GetDailyBalanceUseCase;
import com.findu.transaction.application.port.in.ManageDailyBalanceUseCase;
import com.findu.transaction.application.port.in.command.CalculateDailyBalanceCommand;
import com.findu.transaction.application.port.in.query.GetDailyBalanceQuery;
import com.findu.transaction.application.port.out.persistence.DailyBalanceRepository;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManageDailyBalanceService implements 
        CalculateDailyBalanceUseCase, 
        GetDailyBalanceUseCase, 
        ManageDailyBalanceUseCase {

    private final DailyBalanceRepository dailyBalanceRepository;
    private final ServiceTransactionRepository serviceTransactionRepository;
    private final ProviderAccountRepository providerAccountRepository;

    @Override
    public DailyBalance calculateForProvider(CalculateDailyBalanceCommand command) {
        if (command == null || command.getProviderId() == null || command.getDate() == null) {
            throw new IllegalArgumentException("El comando de cálculo de balance diario es obligatorio");
        }
        return calculateDailyBalance(command.getProviderId(), command.getDate());
    }

    @Override
    public List<DailyBalance> calculateForAllProviders(LocalDate date) {
        return calculateAllDailyBalances(date);
    }

    @Override
    public Optional<DailyBalance> execute(GetDailyBalanceQuery query) {
        if (query == null || query.getProviderId() == null || query.getDate() == null) {
            return Optional.empty();
        }
        return dailyBalanceRepository.findByProviderIdAndBalanceDate(query.getProviderId(), query.getDate());
    }

    // Backwards compatibility for ManageDailyBalanceUseCase
    @Override
    public DailyBalance calculateDailyBalance(Long providerId, LocalDate date) {
        if (providerId == null || date == null) {
            throw new IllegalArgumentException("providerId y date son obligatorios");
        }

        List<ServiceTransaction> transactions = serviceTransactionRepository.findByProviderId(providerId);

        Money totalEarnings = Money.ZERO;
        Money totalCommissions = Money.ZERO;
        Money totalTips = Money.ZERO;
        Money totalExtras = Money.ZERO;
        Money cashPayments = Money.ZERO;
        Money digitalPayments = Money.ZERO;

        for (ServiceTransaction tx : transactions) {
            if (tx.getCreatedAt() != null && tx.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(date)) {
                totalEarnings = totalEarnings.add(tx.getProviderAmount());
                totalCommissions = totalCommissions.add(tx.getCommissionAmount());
                totalTips = totalTips.add(tx.getTipAmount());
                totalExtras = totalExtras.add(tx.getExtraAmount());

                if (tx.getPaymentMethod() == PaymentMethod.CASH) {
                    cashPayments = cashPayments.add(tx.getTotalCustomerAmount());
                } else {
                    digitalPayments = digitalPayments.add(tx.getTotalCustomerAmount());
                }
            }
        }

        Optional<ProviderAccount> accountOpt = providerAccountRepository.findByProviderId(providerId);
        Money providerDebt = accountOpt.map(ProviderAccount::getReceivableBalance).orElse(Money.ZERO);
        Money pendingPayout = accountOpt.map(ProviderAccount::getPayableBalance).orElse(Money.ZERO);
        Money closingBalance = pendingPayout.subtract(providerDebt);

        DailyBalance dailyBalance = DailyBalance.create(
                providerId,
                date,
                Money.ZERO,
                totalEarnings,
                totalCommissions,
                totalTips,
                totalExtras,
                cashPayments,
                digitalPayments,
                providerDebt,
                pendingPayout,
                closingBalance
        );

        return dailyBalanceRepository.save(dailyBalance);
    }

    @Override
    public List<DailyBalance> calculateAllDailyBalances(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<ServiceTransaction> allTxs = serviceTransactionRepository.findAll();

        List<Long> providerIds = allTxs.stream()
                .map(ServiceTransaction::getProviderId)
                .distinct()
                .toList();

        List<DailyBalance> results = new ArrayList<>();
        for (Long providerId : providerIds) {
            results.add(calculateDailyBalance(providerId, targetDate));
        }

        return results;
    }
}
