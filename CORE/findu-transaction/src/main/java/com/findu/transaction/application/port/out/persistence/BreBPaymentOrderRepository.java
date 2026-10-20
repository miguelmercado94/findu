package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.model.finance.BreBPaymentOrder;

import java.util.List;
import java.util.Optional;

public interface BreBPaymentOrderRepository {
    BreBPaymentOrder save(BreBPaymentOrder order);
    Optional<BreBPaymentOrder> findById(String id);
    Optional<BreBPaymentOrder> findByOrderCode(String orderCode);
    Optional<BreBPaymentOrder> findByBreBKey(String breBKey);
    List<BreBPaymentOrder> findByProviderId(Long providerId);
}
