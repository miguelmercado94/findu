package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataRefundRepository extends JpaRepository<RefundEntity, String> {
    Optional<RefundEntity> findByRefundCode(String refundCode);
    List<RefundEntity> findByStatus(String status);
    List<RefundEntity> findByCustomerId(Long customerId);
}
