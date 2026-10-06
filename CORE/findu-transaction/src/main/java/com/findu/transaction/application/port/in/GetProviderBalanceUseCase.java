package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetProviderBalanceQuery;
import com.findu.transaction.domain.model.account.ProviderAccount;

import java.util.Optional;

public interface GetProviderBalanceUseCase {
    Optional<ProviderAccount> execute(GetProviderBalanceQuery query);
}
