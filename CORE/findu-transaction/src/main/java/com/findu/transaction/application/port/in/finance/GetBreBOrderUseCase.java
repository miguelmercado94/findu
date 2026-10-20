package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.domain.model.finance.BreBPaymentOrder;

import java.util.Optional;

public interface GetBreBOrderUseCase {
    Optional<BreBPaymentOrder> getOrderById(String orderId);
    Optional<BreBPaymentOrder> getOrderByCode(String orderCode);
}
