package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.enums.RefundStatus;
import com.findu.transaction.domain.model.refund.Refund;

import java.util.List;
import java.util.Optional;

public interface RefundRepository {
    Refund save(Refund refund);
    Optional<Refund> findById(String id);
    Optional<Refund> findByRefundCode(String refundCode);
    List<Refund> findByStatus(RefundStatus status);
    List<Refund> findByCustomerId(Long customerId);
}
