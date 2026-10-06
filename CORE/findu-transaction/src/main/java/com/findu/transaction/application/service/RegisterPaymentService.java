package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.RegisterPaymentUseCase;
import com.findu.transaction.application.port.in.command.RegisterPaymentCommand;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.domain.event.PaymentCompletedEvent;
import com.findu.transaction.domain.event.TransactionCreatedEvent;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegisterPaymentService implements RegisterPaymentUseCase {

    private final ServiceTransactionRepository serviceTransactionRepository;
    private final EventPublisherPort eventPublisher;
    private final TransactionEventPublisher transactionEventPublisher;

    @Override
    public ServiceTransaction execute(RegisterPaymentCommand command) {
        if (command == null || command.getTransactionId() == null) {
            throw new IllegalArgumentException("El comando de pago no puede ser nulo");
        }

        ServiceTransaction transaction = serviceTransactionRepository.findById(command.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada con id: " + command.getTransactionId()));

        ServiceTransaction updatedTx = serviceTransactionRepository.save(transaction);

        eventPublisher.publish(new TransactionCreatedEvent(
                updatedTx.getId(),
                updatedTx.getTransactionCode().getValue(),
                updatedTx.getServiceRequestId(),
                updatedTx.getProviderId(),
                updatedTx.getCustomerId(),
                updatedTx.getTotalCustomerAmount(),
                updatedTx.getProviderAmount(),
                updatedTx.getPaymentMethod()
        ));

        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
                "PAY-" + updatedTx.getId(),
                updatedTx.getId(),
                updatedTx.getCustomerId(),
                updatedTx.getProviderId(),
                updatedTx.getTotalCustomerAmount(),
                updatedTx.getPaymentMethod(),
                command.getReference(),
                Instant.now()
        );

        if (transactionEventPublisher != null) {
            transactionEventPublisher.publish(paymentEvent.toEnvelope(null));
        }

        return updatedTx;
    }
}
