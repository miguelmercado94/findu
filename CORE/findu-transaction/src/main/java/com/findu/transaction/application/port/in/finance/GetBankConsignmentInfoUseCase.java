package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.application.port.in.finance.dto.BankConsignmentInfoResult;

public interface GetBankConsignmentInfoUseCase {
    BankConsignmentInfoResult getBankConsignmentInfo(Long providerId, String username);
}
