package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.PaymentTransferEntity;

import java.util.Optional;

public interface PaymentTransferRepository {
    PaymentTransferEntity save(PaymentTransferEntity transfer);
    Optional<PaymentTransferEntity> findById(String id);
    Optional<PaymentTransferEntity> findByReference(String reference);
}
