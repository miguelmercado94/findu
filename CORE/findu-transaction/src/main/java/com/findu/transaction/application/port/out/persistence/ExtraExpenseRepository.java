package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.model.expense.ExtraExpenseRequest;

import java.util.List;
import java.util.Optional;

public interface ExtraExpenseRepository {
    ExtraExpenseRequest save(ExtraExpenseRequest extraExpense);
    Optional<ExtraExpenseRequest> findById(String id);
    List<ExtraExpenseRequest> findByServiceRequestId(Long serviceRequestId);
    List<ExtraExpenseRequest> findByProviderId(Long providerId);
}
