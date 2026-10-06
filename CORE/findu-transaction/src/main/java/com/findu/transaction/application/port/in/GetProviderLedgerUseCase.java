package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetProviderLedgerQuery;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;

import java.util.List;

public interface GetProviderLedgerUseCase {
    List<ProviderLedgerEntry> execute(GetProviderLedgerQuery query);
}
