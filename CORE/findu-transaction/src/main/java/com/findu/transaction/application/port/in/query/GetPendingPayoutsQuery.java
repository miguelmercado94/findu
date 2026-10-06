package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetPendingPayoutsQuery {
    private final Long providerId;
}
