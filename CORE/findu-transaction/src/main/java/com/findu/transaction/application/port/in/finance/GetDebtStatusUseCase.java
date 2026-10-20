package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.application.port.in.finance.dto.DebtStatusResult;

public interface GetDebtStatusUseCase {
    DebtStatusResult getDebtStatus(Long providerId);
}
