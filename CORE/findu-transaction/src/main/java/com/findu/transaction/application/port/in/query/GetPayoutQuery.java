package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetPayoutQuery {
    private final String payoutId;
    private final String payoutCode;
    private final Long providerId;
}
