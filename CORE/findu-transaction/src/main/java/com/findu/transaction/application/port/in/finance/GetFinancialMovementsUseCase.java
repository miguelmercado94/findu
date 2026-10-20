package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.application.port.in.finance.dto.FinancialMovementsResult;

public interface GetFinancialMovementsUseCase {
    FinancialMovementsResult getMovements(Long providerId, String filter);
}
