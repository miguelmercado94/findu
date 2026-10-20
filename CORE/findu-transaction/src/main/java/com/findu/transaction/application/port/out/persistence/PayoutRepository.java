package com.findu.transaction.application.port.out.persistence;

import com.findu.transaction.domain.enums.PayoutStatus;
import com.findu.transaction.domain.model.payout.Payout;

import java.util.List;
import java.util.Optional;

public interface PayoutRepository {
    Payout save(Payout payout);
    Optional<Payout> findById(String id);
    Optional<Payout> findByPayoutCode(String payoutCode);
    List<Payout> findByStatus(PayoutStatus status);
    List<Payout> findByProviderId(Long providerId);
}
