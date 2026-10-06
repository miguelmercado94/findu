package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetTransactionQuery;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;

import java.util.Optional;

public interface GetTransactionUseCase {
    Optional<ServiceTransaction> execute(GetTransactionQuery query);
}
