package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.CreateServiceTransactionUseCase;
import com.findu.transaction.application.port.in.command.CreateServiceTransactionCommand;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.event.TransactionCreatedEvent;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateServiceTransactionService implements CreateServiceTransactionUseCase {

    private final ServiceTransactionRepository serviceTransactionRepository;
    private final ProviderAccountRepository providerAccountRepository;
    private final EventPublisherPort eventPublisher;

    @Override
    public ServiceTransaction execute(CreateServiceTransactionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando de creación de transacción no puede ser nulo");
        }

        // Crear la entidad de transacción de servicio
        ServiceTransaction transaction = ServiceTransaction.create(
                command.getServiceRequestId(),
                command.getProviderId(),
                command.getCustomerId(),
                command.getServiceAmount(),
                command.getCommissionRate(),
                command.getTipAmount(),
                command.getExtraAmount(),
                command.getPaymentMethod()
        );

        // Guardar la transacción
        ServiceTransaction savedTx = serviceTransactionRepository.save(transaction);

        // Actualizar o crear la cuenta del proveedor (ProviderAccount)
        ProviderAccount account = providerAccountRepository.findByProviderId(command.getProviderId())
                .orElseGet(() -> ProviderAccount.create(command.getProviderId(), ProviderType.PERSONA_NATURAL));

        account.recordServiceTransaction(savedTx);
        providerAccountRepository.save(account);

        // Publicar evento de dominio
        eventPublisher.publish(new TransactionCreatedEvent(
                savedTx.getId(),
                savedTx.getTransactionCode().getValue(),
                savedTx.getServiceRequestId(),
                savedTx.getProviderId(),
                savedTx.getCustomerId(),
                savedTx.getTotalCustomerAmount(),
                savedTx.getProviderAmount(),
                savedTx.getPaymentMethod()
        ));

        return savedTx;
    }
}
