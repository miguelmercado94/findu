package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.PaymentTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataPaymentTransferRepository extends JpaRepository<PaymentTransferEntity, String> {
    Optional<PaymentTransferEntity> findByReference(String reference);
}
