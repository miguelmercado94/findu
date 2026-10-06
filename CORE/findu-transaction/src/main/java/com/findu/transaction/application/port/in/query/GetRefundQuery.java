package com.findu.transaction.application.port.in.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetRefundQuery {
    private final String refundId;
    private final String refundCode;
    private final String transactionId;
}
