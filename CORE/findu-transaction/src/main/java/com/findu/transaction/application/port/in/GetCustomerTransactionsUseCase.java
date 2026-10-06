package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetCustomerTransactionsQuery;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;

import java.util.List;

public interface GetCustomerTransactionsUseCase {
    List<ServiceTransaction> execute(GetCustomerTransactionsQuery query);
}
