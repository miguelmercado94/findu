package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetProviderDebtQuery;
import com.findu.transaction.domain.valueobject.Money;

public interface GetProviderDebtUseCase {
    Money execute(GetProviderDebtQuery query);
}
