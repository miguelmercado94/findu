package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.application.port.in.finance.command.ProcessBreBWebhookCommand;
import com.findu.transaction.domain.model.finance.BreBPaymentOrder;

public interface ProcessBreBWebhookUseCase {
    BreBPaymentOrder processWebhook(ProcessBreBWebhookCommand command);
}
