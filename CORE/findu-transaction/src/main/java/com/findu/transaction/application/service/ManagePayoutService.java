package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.CreatePayoutUseCase;
import com.findu.transaction.application.port.in.ExecutePayoutUseCase;
import com.findu.transaction.application.port.in.GetPayoutUseCase;
import com.findu.transaction.application.port.in.ManagePayoutUseCase;
import com.findu.transaction.application.port.in.command.CreatePayoutCommand;
import com.findu.transaction.application.port.in.command.ExecutePayoutCommand;
import com.findu.transaction.application.port.in.query.GetPayoutQuery;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.persistence.PayoutRepository;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.transfer.TransferPort;
import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.enums.PayoutStatus;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import com.findu.transaction.domain.event.PayoutCompletedEvent;
import com.findu.transaction.domain.event.PayoutCreatedEvent;
import com.findu.transaction.domain.event.PayoutFailedEvent;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManagePayoutService implements 
        CreatePayoutUseCase, 
        ExecutePayoutUseCase, 
        GetPayoutUseCase, 
        ManagePayoutUseCase {

    private final PayoutRepository payoutRepository;
    private final ProviderAccountRepository providerAccountRepository;
    private final TransferPort transferPort;
    private final EventPublisherPort eventPublisher;
    private final TransactionEventPublisher transactionEventPublisher;

    @Override
    public Payout execute(CreatePayoutCommand command) {
        return createPayout(command);
    }

    @Override
    public Payout createPayout(CreatePayoutCommand command) {
        if (command == null || command.getProviderId() == null) {
            throw new IllegalArgumentException("El comando de payout es obligatorio");
        }

        ProviderAccount account = providerAccountRepository.findByProviderId(command.getProviderId())
                .orElseGet(() -> ProviderAccount.create(command.getProviderId(), ProviderType.PERSONA_NATURAL));

        account.applyCompensation();

        Money payoutAmount = command.getAmount() != null ? command.getAmount() : account.getPayableBalance();
        if (!payoutAmount.isPositive()) {
            throw new IllegalStateException("El saldo disponible para Payout es $0 después de compensación de deudas.");
        }

        Payout payout = Payout.create(command.getSettlementId(), command.getProviderId(), payoutAmount);
        Payout savedPayout = payoutRepository.save(payout);

        account.recordPayout(payoutAmount, savedPayout.getPayoutCode());
        providerAccountRepository.save(account);

        eventPublisher.publish(new PayoutCreatedEvent(
                savedPayout.getId(),
                savedPayout.getPayoutCode(),
                savedPayout.getProviderId(),
                savedPayout.getAmount()
        ));

        if (transactionEventPublisher != null) {
            PayoutCreatedEvent createdEvent = new PayoutCreatedEvent(
                    savedPayout.getId(),
                    savedPayout.getSettlementId(),
                    savedPayout.getProviderId(),
                    savedPayout.getAmount(),
                    PaymentMethod.SIMULATED,
                    Instant.now()
            );
            transactionEventPublisher.publish(createdEvent.toEnvelope(null));
        }

        return savedPayout;
    }

    @Override
    public Payout execute(ExecutePayoutCommand command) {
        if (command == null || command.getPayoutId() == null) {
            throw new IllegalArgumentException("El comando de ejecución de Payout no puede ser nulo");
        }
        return executePayout(command.getPayoutId(), command.getOutcome());
    }

    @Override
    public Payout executePayout(String payoutId, TransferSimulationOutcome outcome) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout no encontrado con id: " + payoutId));

        if (payout.getStatus() == PayoutStatus.COMPLETED) {
            return payout;
        }

        payout.markProcessing();
        payoutRepository.save(payout);

        TransferSimulationOutcome targetOutcome = outcome != null ? outcome : TransferSimulationOutcome.SUCCESS;
        TransferPort.TransferResult result = transferPort.transfer(
                payout.getPayoutCode(),
                payout.getProviderId(),
                payout.getAmount(),
                targetOutcome
        );

        if (result.isSuccessful()) {
            payout.markCompleted(result.externalTransactionId());
            Payout updated = payoutRepository.save(payout);

            eventPublisher.publish(new PayoutCompletedEvent(
                    updated.getId(),
                    updated.getPayoutCode(),
                    updated.getProviderId(),
                    updated.getAmount(),
                    updated.getExternalReference()
            ));

            if (transactionEventPublisher != null) {
                PayoutCompletedEvent completedEvent = new PayoutCompletedEvent(
                        updated.getId(),
                        updated.getSettlementId(),
                        updated.getProviderId(),
                        updated.getAmount(),
                        updated.getExternalReference(),
                        Instant.now()
                );
                transactionEventPublisher.publish(completedEvent.toEnvelope(null));
            }

            return updated;
        } else {
            payout.markFailed(result.message());
            Payout failedPayout = payoutRepository.save(payout);

            if (transactionEventPublisher != null) {
                PayoutFailedEvent failedEvent = new PayoutFailedEvent(
                        failedPayout.getId(),
                        failedPayout.getSettlementId(),
                        failedPayout.getProviderId(),
                        failedPayout.getAmount(),
                        result.message()
                );
                transactionEventPublisher.publish(failedEvent.toEnvelope(null));
            }

            return failedPayout;
        }
    }

    @Override
    public Optional<Payout> execute(GetPayoutQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        if (query.getPayoutId() != null) {
            return payoutRepository.findById(query.getPayoutId());
        }
        if (query.getPayoutCode() != null) {
            return payoutRepository.findByPayoutCode(query.getPayoutCode());
        }
        return Optional.empty();
    }

    @Override
    public List<Payout> getPendingPayouts() {
        return payoutRepository.findByStatus(PayoutStatus.PENDING);
    }
}
