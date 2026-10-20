package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.BreBPaymentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataBreBPaymentOrderRepository extends JpaRepository<BreBPaymentOrderEntity, String> {
    Optional<BreBPaymentOrderEntity> findByOrderCode(String orderCode);
    Optional<BreBPaymentOrderEntity> findByBreBKey(String breBKey);
    List<BreBPaymentOrderEntity> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
