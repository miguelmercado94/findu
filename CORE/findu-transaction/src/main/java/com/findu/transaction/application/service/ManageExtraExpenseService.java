package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.ApproveExtraExpenseUseCase;
import com.findu.transaction.application.port.in.GetExtraExpenseUseCase;
import com.findu.transaction.application.port.in.ManageExtraExpenseUseCase;
import com.findu.transaction.application.port.in.RejectExtraExpenseUseCase;
import com.findu.transaction.application.port.in.RequestExtraExpenseUseCase;
import com.findu.transaction.application.port.in.command.ApproveExtraExpenseCommand;
import com.findu.transaction.application.port.in.command.RejectExtraExpenseCommand;
import com.findu.transaction.application.port.in.command.RequestExtraExpenseCommand;
import com.findu.transaction.application.port.in.query.GetExtraExpenseQuery;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.persistence.ExtraExpenseRepository;
import com.findu.transaction.domain.event.ExtraExpenseApprovedEvent;
import com.findu.transaction.domain.event.ExtraExpenseRejectedEvent;
import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManageExtraExpenseService implements 
        RequestExtraExpenseUseCase, 
        ApproveExtraExpenseUseCase, 
        RejectExtraExpenseUseCase, 
        GetExtraExpenseUseCase, 
        ManageExtraExpenseUseCase {

    private final ExtraExpenseRepository extraExpenseRepository;
    private final EventPublisherPort eventPublisher;
    private final TransactionEventPublisher transactionEventPublisher;

    @Override
    public ExtraExpenseRequest execute(RequestExtraExpenseCommand command) {
        return requestExpense(command);
    }

    @Override
    public ExtraExpenseRequest requestExpense(RequestExtraExpenseCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }
        ExtraExpenseRequest request = ExtraExpenseRequest.request(
                command.getServiceRequestId(),
                command.getProviderId(),
                command.getAmount(),
                command.getReason()
        );
        return extraExpenseRepository.save(request);
    }

    @Override
    public ExtraExpenseRequest execute(ApproveExtraExpenseCommand command) {
        if (command == null || command.getExpenseId() == null) {
            throw new IllegalArgumentException("El comando de aprobación es obligatorio");
        }
        ExtraExpenseRequest expense = extraExpenseRepository.findById(command.getExpenseId())
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de gasto extra no encontrada con id: " + command.getExpenseId()));
        
        expense.approve();
        ExtraExpenseRequest saved = extraExpenseRepository.save(expense);

        eventPublisher.publish(new ExtraExpenseApprovedEvent(
                saved.getId(),
                saved.getServiceRequestId(),
                saved.getProviderId(),
                saved.getAmount()
        ));

        if (transactionEventPublisher != null) {
            ExtraExpenseApprovedEvent approvedEvent = new ExtraExpenseApprovedEvent(
                    saved.getId(),
                    String.valueOf(saved.getServiceRequestId()),
                    saved.getProviderId(),
                    saved.getAmount(),
                    Instant.now()
            );
            transactionEventPublisher.publish(approvedEvent.toEnvelope(null));
        }

        return saved;
    }

    @Override
    public ExtraExpenseRequest execute(RejectExtraExpenseCommand command) {
        if (command == null || command.getExpenseId() == null) {
            throw new IllegalArgumentException("El comando de rechazo es obligatorio");
        }
        ExtraExpenseRequest expense = extraExpenseRepository.findById(command.getExpenseId())
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de gasto extra no encontrada con id: " + command.getExpenseId()));
        
        expense.reject();
        ExtraExpenseRequest rejected = extraExpenseRepository.save(expense);

        if (transactionEventPublisher != null) {
            ExtraExpenseRejectedEvent rejectedEvent = new ExtraExpenseRejectedEvent(
                    rejected.getId(),
                    String.valueOf(rejected.getServiceRequestId()),
                    rejected.getProviderId(),
                    rejected.getReason() != null ? rejected.getReason() : "Rechazado por cliente/sistema",
                    Instant.now()
            );
            transactionEventPublisher.publish(rejectedEvent.toEnvelope(null));
        }

        return rejected;
    }

    @Override
    public Optional<ExtraExpenseRequest> execute(GetExtraExpenseQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        if (query.getExpenseId() != null) {
            return extraExpenseRepository.findById(query.getExpenseId());
        }
        if (query.getServiceRequestId() != null) {
            return extraExpenseRepository.findByServiceRequestId(query.getServiceRequestId()).stream().findFirst();
        }
        return Optional.empty();
    }

    @Override
    public ExtraExpenseRequest approveExpense(String expenseId) {
        return execute(ApproveExtraExpenseCommand.builder().expenseId(expenseId).build());
    }

    @Override
    public ExtraExpenseRequest rejectExpense(String expenseId) {
        return execute(RejectExtraExpenseCommand.builder().expenseId(expenseId).build());
    }
}
