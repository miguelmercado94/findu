package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetProviderBalanceQuery {
    private final Long providerId;
}
