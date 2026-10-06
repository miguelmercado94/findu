package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetSettlementQuery {
    private final String settlementId;
    private final Long providerId;
}
