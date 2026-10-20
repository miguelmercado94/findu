package com.findu.transaction.application.port.in.finance.command;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProcessBreBWebhookCommand {
    private final String orderId;
    private final String orderCode;
    private final String transactionId;
    private final Money amount;
    private final String status;
    private final String signature;
    private final String rawPayload;
}
