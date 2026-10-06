package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetTransactionQuery {
    private final String transactionId;
    private final String transactionCode;
}
