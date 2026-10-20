package com.findu.transaction.infrastructure.adapter.out.persistence.adapter;

import com.findu.transaction.application.port.out.persistence.PaymentTransferRepository;
import com.findu.transaction.infrastructure.adapter.out.persistence.entity.PaymentTransferEntity;
import com.findu.transaction.infrastructure.adapter.out.persistence.repository.SpringDataPaymentTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaPaymentTransferAdapter implements PaymentTransferRepository {

    private final SpringDataPaymentTransferRepository jpaRepository;

    @Override
    public PaymentTransferEntity save(PaymentTransferEntity transfer) {
        return jpaRepository.save(transfer);
    }

    @Override
    public Optional<PaymentTransferEntity> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<PaymentTransferEntity> findByReference(String reference) {
        return jpaRepository.findByReference(reference);
    }
}
