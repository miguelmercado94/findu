package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetProviderLedgerQuery {
    private final Long providerId;
    private final Integer limit;
}
