package com.findu.transaction.infrastructure.adapter.out.persistence.repository;

import com.findu.transaction.infrastructure.adapter.out.persistence.entity.ExtraExpenseRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataExtraExpenseRepository extends JpaRepository<ExtraExpenseRequestEntity, String> {
    List<ExtraExpenseRequestEntity> findByServiceRequestId(Long serviceRequestId);
    List<ExtraExpenseRequestEntity> findByProviderId(Long providerId);
}
