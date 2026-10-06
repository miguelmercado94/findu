package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.CreateRefundUseCase;
import com.findu.transaction.application.port.in.ExecuteRefundUseCase;
import com.findu.transaction.application.port.in.GetRefundUseCase;
import com.findu.transaction.application.port.in.ManageRefundUseCase;
import com.findu.transaction.application.port.in.command.CreateRefundCommand;
import com.findu.transaction.application.port.in.command.ExecuteRefundCommand;
import com.findu.transaction.application.port.in.query.GetRefundQuery;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.persistence.RefundRepository;
import com.findu.transaction.application.port.out.transfer.TransferPort;
import com.findu.transaction.domain.enums.RefundStatus;
import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import com.findu.transaction.domain.event.RefundCompletedEvent;
import com.findu.transaction.domain.event.RefundCreatedEvent;
import com.findu.transaction.domain.event.RefundFailedEvent;
import com.findu.transaction.domain.event.RefundRequestedEvent;
import com.findu.transaction.domain.model.refund.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManageRefundService implements 
        CreateRefundUseCase, 
        ExecuteRefundUseCase, 
        GetRefundUseCase, 
        ManageRefundUseCase {

    private final RefundRepository refundRepository;
    private final TransferPort transferPort;
    private final EventPublisherPort eventPublisher;
    private final TransactionEventPublisher transactionEventPublisher;

    @Override
    public Refund execute(CreateRefundCommand command) {
        return createRefund(command);
    }

    @Override
    public Refund createRefund(CreateRefundCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando de refund es obligatorio");
        }
        Refund refund = Refund.create(
                command.getTransactionId(),
                command.getCustomerId(),
                command.getAmount(),
                command.getReason()
        );
        Refund saved = refundRepository.save(refund);

        eventPublisher.publish(new RefundRequestedEvent(
                saved.getId(),
                saved.getRefundCode(),
                saved.getTransactionId(),
                saved.getCustomerId(),
                saved.getAmount()
        ));

        if (transactionEventPublisher != null) {
            RefundCreatedEvent createdEvent = new RefundCreatedEvent(
                    saved.getId(),
                    saved.getTransactionId(),
                    saved.getCustomerId(),
                    saved.getAmount(),
                    saved.getReason()
            );
            transactionEventPublisher.publish(createdEvent.toEnvelope(null));
        }

        return saved;
    }

    @Override
    public Refund execute(ExecuteRefundCommand command) {
        if (command == null || command.getRefundId() == null) {
            throw new IllegalArgumentException("El comando de ejecución de Refund no puede ser nulo");
        }
        return executeRefund(command.getRefundId(), command.getOutcome());
    }

    @Override
    public Refund executeRefund(String refundId, TransferSimulationOutcome outcome) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund no encontrado con id: " + refundId));

        if (refund.getStatus() == RefundStatus.COMPLETED) {
            return refund; // Idempotencia
        }

        refund.markProcessing();
        refundRepository.save(refund);

        TransferSimulationOutcome targetOutcome = outcome != null ? outcome : TransferSimulationOutcome.SUCCESS;
        TransferPort.TransferResult result = transferPort.transfer(
                refund.getRefundCode(),
                refund.getCustomerId(),
                refund.getAmount(),
                targetOutcome
        );

        if (result.isSuccessful()) {
            refund.markCompleted(result.externalTransactionId());
            Refund updated = refundRepository.save(refund);

            eventPublisher.publish(new RefundCompletedEvent(
                    updated.getId(),
                    updated.getRefundCode(),
                    updated.getCustomerId(),
                    updated.getAmount(),
                    updated.getExternalReference()
            ));

            if (transactionEventPublisher != null) {
                RefundCompletedEvent completedEvent = new RefundCompletedEvent(
                        updated.getId(),
                        updated.getTransactionId(),
                        updated.getCustomerId(),
                        updated.getAmount(),
                        updated.getExternalReference(),
                        Instant.now()
                );
                transactionEventPublisher.publish(completedEvent.toEnvelope(null));
            }

            return updated;
        } else {
            refund.markFailed(result.message());
            Refund failedRefund = refundRepository.save(refund);

            if (transactionEventPublisher != null) {
                RefundFailedEvent failedEvent = new RefundFailedEvent(
                        failedRefund.getId(),
                        failedRefund.getTransactionId(),
                        failedRefund.getCustomerId(),
                        failedRefund.getAmount(),
                        result.message()
                );
                transactionEventPublisher.publish(failedEvent.toEnvelope(null));
            }

            return failedRefund;
        }
    }

    @Override
    public Optional<Refund> execute(GetRefundQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        if (query.getRefundId() != null) {
            return refundRepository.findById(query.getRefundId());
        }
        if (query.getRefundCode() != null) {
            return refundRepository.findByRefundCode(query.getRefundCode());
        }
        return Optional.empty();
    }

    @Override
    public List<Refund> getPendingRefunds() {
        return refundRepository.findByStatus(RefundStatus.PENDING);
    }
}
