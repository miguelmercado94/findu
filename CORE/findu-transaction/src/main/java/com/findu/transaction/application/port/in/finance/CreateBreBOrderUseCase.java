package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.application.port.in.finance.command.CreateBreBOrderCommand;
import com.findu.transaction.domain.model.finance.BreBPaymentOrder;

public interface CreateBreBOrderUseCase {
    BreBPaymentOrder createOrder(CreateBreBOrderCommand command);
}
