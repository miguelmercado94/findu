package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetExtraExpenseQuery {
    private final String expenseId;
    private final Long serviceRequestId;
    private final Long providerId;
}
