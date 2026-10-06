package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetCustomerTransactionsQuery {
    private final Long customerId;
    private final Integer page;
    private final Integer size;
}
