package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.ManageProviderDebtUseCase;
import com.findu.transaction.application.port.in.PayProviderDebtUseCase;
import com.findu.transaction.application.port.in.command.PayProviderDebtCommand;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.ProviderDebtPaymentRepository;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.event.ProviderDebtPaidEvent;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.debt.ProviderDebtPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ManageProviderDebtService implements PayProviderDebtUseCase, ManageProviderDebtUseCase {

    private final ProviderDebtPaymentRepository providerDebtPaymentRepository;
    private final ProviderAccountRepository providerAccountRepository;
    private final EventPublisherPort eventPublisher;
    private final TransactionEventPublisher transactionEventPublisher;

    @Override
    public ProviderDebtPayment execute(PayProviderDebtCommand command) {
        return payDebt(command);
    }

    @Override
    public ProviderDebtPayment payDebt(PayProviderDebtCommand command) {
        if (command == null || command.getProviderId() == null || command.getAmount() == null) {
            throw new IllegalArgumentException("El comando de pago de deuda debe incluir providerId y monto");
        }

        ProviderDebtPayment payment = ProviderDebtPayment.create(
                command.getProviderId(),
                command.getAmount(),
                command.getPaymentMethod(),
                command.getExternalReference()
        );
        ProviderDebtPayment savedPayment = providerDebtPaymentRepository.save(payment);

        ProviderAccount account = providerAccountRepository.findByProviderId(command.getProviderId())
                .orElseGet(() -> ProviderAccount.create(command.getProviderId(), ProviderType.PERSONA_NATURAL));

        account.recordDebtPayment(command.getAmount(), savedPayment.getExternalReference());
        providerAccountRepository.save(account);

        ProviderDebtPaidEvent debtEvent = new ProviderDebtPaidEvent(
                savedPayment.getId(),
                savedPayment.getProviderId(),
                savedPayment.getAmount(),
                savedPayment.getPaymentMethod(),
                savedPayment.getExternalReference(),
                Instant.now()
        );

        eventPublisher.publish(debtEvent);

        if (transactionEventPublisher != null) {
            transactionEventPublisher.publish(debtEvent.toEnvelope(null));
        }

        return savedPayment;
    }
}
